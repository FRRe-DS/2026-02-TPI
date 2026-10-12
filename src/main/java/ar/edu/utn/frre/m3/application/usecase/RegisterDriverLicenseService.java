package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.IdempotencyRecord;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RegisterDriverLicenseService implements RegisterDriverLicenseUseCase {
    private final DriverRepository drivers;
    private final DocumentRepository documents;
    private final UnitOfWork unitOfWork;
    private final IdempotencyRepository idempotency;

    public RegisterDriverLicenseService(DriverRepository drivers, DocumentRepository documents, UnitOfWork unitOfWork) {
        this(drivers, documents, unitOfWork, null);
    }

    public RegisterDriverLicenseService(DriverRepository drivers, DocumentRepository documents,
                                        UnitOfWork unitOfWork, IdempotencyRepository idempotency) {
        this.drivers = Objects.requireNonNull(drivers);
        this.documents = Objects.requireNonNull(documents);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.idempotency = idempotency;
    }

    @Override
    public Document register(RegisterDriverLicenseCommand command) {
        Objects.requireNonNull(command);
        return unitOfWork.executeInTransaction(() -> {
            // The driver lock serializes concurrent renewals and protects the revision number.
            Driver driver = drivers.findByIdWithPessimisticLock(command.driverId())
                    .orElseThrow(() -> new DriverNotFoundException(command.driverId()));
            String path = "/api/v1/drivers/" + driver.getId() + "/documents";
            if (command.idempotencyKey() != null) {
                IdempotencyRecord existing = idempotency.findByKeyAndMethodAndPath(
                        command.idempotencyKey(), "POST", path).orElse(null);
                if (existing != null) {
                    if (!existing.getRequestHash().equals(command.requestHash())) {
                        throw new IdempotencyConflictException("Idempotency-Key was reused with different license data");
                    }
                    UUID documentId = UUID.fromString(existing.getResponseBody());
                    return documents.findById(documentId)
                            .orElseThrow(() -> new DocumentNotFoundException(documentId));
                }
            }
            driver.validateVersion(command.expectedVersion());

            Document previous = documents.findCurrentLicenseByDriverId(driver.getId()).orElse(null);
            int revision = previous == null
                    ? documents.findLicensesByDriverId(driver.getId()).stream()
                            .mapToInt(Document::getRevision).max().orElse(0) + 1
                    : previous.getRevision() + 1;
            Document next = Document.createPendingLicense(driver.getId(), command.documentNumber(),
                    command.issuedAt(), command.validUntil(), command.serviceTypes(),
                    command.storageReference(), revision, driver.getVersion() + 1);

            if (previous != null) {
                previous.markAsNonCurrent();
                documents.save(previous);
            }
            driver.incrementVersion();
            drivers.save(driver);
            Document created = documents.save(next);
            if (command.idempotencyKey() != null) {
                idempotency.save(new IdempotencyRecord(UUID.randomUUID(), command.idempotencyKey(),
                        driver.getId().toString(), "POST", path, command.requestHash(),
                        201, created.getId().toString(), Instant.now()));
            }
            return created;
        });
    }

    @Override
    public List<Document> history(UUID driverId) {
        driver(driverId);
        return documents.findLicensesByDriverId(driverId);
    }

    @Override
    public Driver driver(UUID driverId) {
        return drivers.findById(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
    }
}
