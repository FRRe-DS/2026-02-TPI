package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.domain.IdempotencyRecord;
import ar.edu.utn.frre.m3.application.port.OutboxRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.IntentStatus;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotQualifiedException;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service that implements {@link RequestAvailabilityUseCase} (HU-10).
 *
 * <h2>Flow</h2>
 * <pre>
 * 1. Idempotency check  — replay cached response if same key+hash; 409 on key+different-hash.
 * 2. Driver validation  — 404 if not found; 422 if not approved or lacks own qualified vehicle.
 * 3. Domain operation   — create new intent or update existing one.
 * 4. Atomic transaction — persist intent + audit entry + outbox entry + idempotency record.
 * 5. Return 202 result  — worker will deliver the outbox entry to M4 asynchronously.
 * </pre>
 *
 * <p>This class has <strong>no Spring, JPA or HTTP dependencies</strong>. It is wired
 * as a plain bean in {@code ApplicationUseCaseConfig}.
 */
public class RequestAvailabilityService implements RequestAvailabilityUseCase {

    private final DriverRepository driverRepository;
    private final AvailabilityIntentRepository intentRepository;
    private final AuditLogRepository auditLogRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final ar.edu.utn.frre.m3.application.port.DocumentRepository documents;
    private final ar.edu.utn.frre.m3.application.port.VehicleRepository vehicles;

    public RequestAvailabilityService(
            DriverRepository driverRepository,
            AvailabilityIntentRepository intentRepository,
            AuditLogRepository auditLogRepository,
            OutboxRepository outboxRepository,
            IdempotencyRepository idempotencyRepository,
            UnitOfWork unitOfWork,
            Clock clock) {

        this(driverRepository,intentRepository,auditLogRepository,outboxRepository,idempotencyRepository,unitOfWork,clock,null,null);
    }
    public RequestAvailabilityService(DriverRepository driverRepository,AvailabilityIntentRepository intentRepository,
            AuditLogRepository auditLogRepository,OutboxRepository outboxRepository,IdempotencyRepository idempotencyRepository,
            UnitOfWork unitOfWork,Clock clock,ar.edu.utn.frre.m3.application.port.DocumentRepository documents,
            ar.edu.utn.frre.m3.application.port.VehicleRepository vehicles) {
        this.documents=documents;this.vehicles=vehicles;
        this.driverRepository = Objects.requireNonNull(driverRepository);
        this.intentRepository = Objects.requireNonNull(intentRepository);
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Use-case entry point
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public RequestAvailabilityResult execute(RequestAvailabilityCommand cmd) {
        Objects.requireNonNull(cmd, "command cannot be null");
        if(cmd.idempotencyKey()==null || cmd.idempotencyKey().length()<16 || cmd.idempotencyKey().length()>128)
            throw new IllegalArgumentException("Idempotency-Key debe tener entre 16 y 128 caracteres.");
        if(documents==null) return executeLocked(cmd);
        return unitOfWork.executeInTransaction(() -> {
            var driver=driverRepository.findByIdWithPessimisticLock(cmd.driverId()).orElseThrow(() -> new DriverNotFoundException(cmd.driverId()));
            var result=executeLocked(cmd);
            if(!result.fromCache()) { driver.incrementVersion(); driverRepository.save(driver); }
            return result;
        });
    }
    private RequestAvailabilityResult executeLocked(RequestAvailabilityCommand cmd) {

        // ── Step 1: Idempotency check (outside transaction — read-only) ──────
        Optional<IdempotencyRecord> existing = idempotencyRepository.findByKey(
                cmd.principal(),
                "PUT",
                "/api/v1/drivers/" + cmd.driverId() + "/availability-intent",
                cmd.idempotencyKey());

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.requestHash().equals(cmd.requestHash())) {
                throw new IdempotencyConflictException(cmd.idempotencyKey());
            }
            if(!record.getPrincipal().equals(cmd.principal())) throw new IdempotencyConflictException(cmd.idempotencyKey());
            if(!record.getResponseBody().isBlank()) {
                String[] data=record.getResponseBody().split("\\|",-1);
                return new RequestAvailabilityResult(java.util.UUID.fromString(data[0]),cmd.driverId(),java.util.UUID.fromString(data[1]),
                    Boolean.parseBoolean(data[2]),Boolean.parseBoolean(data[3]),IntentStatus.valueOf(data[4]),Long.parseLong(data[5]),Instant.parse(data[6]),true);
            }
            // Legacy entries created before response snapshots were persisted.
            return intentRepository
                    .findByDriverId(cmd.driverId())
                    .map(intent -> toResult(intent, true))
                    .orElseThrow(() -> new DriverNotFoundException(cmd.driverId()));
        }

        // ── Step 2: Validate driver (pre-transaction, read-only) ─────────────
        if (!driverRepository.existsById(cmd.driverId())) {
            throw new DriverNotFoundException(cmd.driverId());
        }
        if ((cmd.connected() || cmd.available()) && !driverRepository.isApproved(cmd.driverId())) {
            throw new DriverNotQualifiedException(cmd.driverId(),
                    "Driver qualification status is not APPROVED");
        }
        if (vehicles!=null && !vehicles.existsByIdAndDriverId(cmd.vehicleId(),cmd.driverId()))
            throw new DriverNotQualifiedException(cmd.driverId(),"El vehículo no pertenece al conductor.");
        if ((cmd.connected() || cmd.available()) && !driverRepository.hasOwnQualifiedVehicle(cmd.driverId(), cmd.vehicleId())) {
            throw new DriverNotQualifiedException(cmd.driverId(),
                    "Vehicle " + cmd.vehicleId() + " does not belong to driver or is not APPROVED");
        }

        if(documents!=null && (cmd.connected() || cmd.available())) {
            Instant checked=clock.instant();
            var license=documents.findCurrentLicenseByDriverId(cmd.driverId()).orElse(null);
            QualificationService.requireValid(license,checked,"licencia");
            QualificationService.requireValid(documents.findCurrentByVehicleIdAndType(cmd.vehicleId(),ar.edu.utn.frre.m3.domain.DocumentType.INSURANCE).orElse(null),checked,"seguro");
            QualificationService.requireValid(documents.findCurrentByVehicleIdAndType(cmd.vehicleId(),ar.edu.utn.frre.m3.domain.DocumentType.VEHICLE_REGISTRATION).orElse(null),checked,"cédula");
            var vehicle=vehicles.findById(cmd.vehicleId()).orElseThrow();
            if(license.getServiceTypes().stream().noneMatch(t->vehicle.getServiceTypes().contains(t.name())))
                throw new DriverNotQualifiedException(cmd.driverId(),"La licencia no habilita el tipo de vehículo.");
        }
        // ── Step 3 + 4: Atomic transaction ───────────────────────────────────
        Instant now = Instant.now(clock);
        AvailabilityIntent savedIntent = unitOfWork.executeInTransaction(() -> {

            // 3a. Create or update the domain intent
            AvailabilityIntent intent = intentRepository
                    .findByDriverId(cmd.driverId())
                    .map(existing_ -> {
                        existing_.update(cmd.vehicleId(), cmd.connected(), cmd.available(), now);
                        return existing_;
                    })
                    .orElseGet(() -> AvailabilityIntent.create(
                            cmd.driverId(), cmd.vehicleId(),
                            cmd.connected(), cmd.available(), now));

            // 3b. Persist intent
            AvailabilityIntent persisted = intentRepository.save(intent);

            // 3c. Audit entry
            String operation = intent.getDesiredVersion() == 1 ? "CREATE_INTENT" : "UPDATE_INTENT";
            auditLogRepository.append(
                    "AvailabilityIntent",
                    persisted.getId(),
                    operation,
                    cmd.principal(),
                    cmd.correlationId(),
                    "connected=" + cmd.connected() + ", available=" + cmd.available() +
                    ", vehicleId=" + cmd.vehicleId() +
                    ", desiredVersion=" + persisted.getDesiredVersion());

            // 3d. Outbox entry (worker will call M4)
            outboxRepository.enqueue(persisted, cmd.correlationId());

            // 3e. Idempotency record
            idempotencyRepository.save(new IdempotencyRecord(
                    cmd.principal(),
                    "PUT",
                    "/api/v1/drivers/" + cmd.driverId() + "/availability-intent",
                    cmd.idempotencyKey(),
                    cmd.requestHash(),
                    202,
                    persisted.getId()+"|"+persisted.getVehicleId()+"|"+persisted.isConnected()+"|"+persisted.isAvailable()+"|"+
                        persisted.getStatus()+"|"+persisted.getDesiredVersion()+"|"+persisted.getUpdatedAt()
            ));

            return persisted;
        });

        // ── Step 5: Return result ─────────────────────────────────────────────
        return toResult(savedIntent, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static RequestAvailabilityResult toResult(AvailabilityIntent intent, boolean fromCache) {
        return new RequestAvailabilityResult(
                intent.getId(),
                intent.getDriverId(),
                intent.getVehicleId(),
                intent.isConnected(),
                intent.isAvailable(),
                intent.getStatus(),
                intent.getDesiredVersion(),
                intent.getUpdatedAt(),
                fromCache);
    }
}
