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
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RequestAvailabilityService} (HU-10).
 *
 * <p>Verifies:
 * <ul>
 *   <li>Idempotency replay on identical key+hash.</li>
 *   <li>409 Conflict on same key + different hash.</li>
 *   <li>404 when driver not found.</li>
 *   <li>422 when driver is not APPROVED.</li>
 *   <li>422 when vehicle does not belong to driver or is not APPROVED.</li>
 *   <li>Happy path creates intent in PENDING state and calls outbox/audit.</li>
 *   <li>Happy path updates existing intent (increments desiredVersion).</li>
 *   <li>All four ports called within a single transaction.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestAvailabilityService — use case tests")
class RequestAvailabilityServiceTest {

    @Mock DriverRepository driverRepository;
    @Mock AvailabilityIntentRepository intentRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock IdempotencyRepository idempotencyRepository;
    @Mock UnitOfWork unitOfWork;

    private static final Instant FIXED_NOW = Instant.parse("2026-10-10T21:00:00Z");
    private final Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));

    private RequestAvailabilityService service;

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final String PRINCIPAL = "driver:" + DRIVER_ID;
    private static final String IDEM_KEY = "test-idem-key-001";
    private static final String REQUEST_HASH = "aabbcc112233";
    private static final String CORRELATION_ID = "corr-001";

    @BeforeEach
    void setUp() {
        service = new RequestAvailabilityService(
                driverRepository, intentRepository, auditLogRepository,
                outboxRepository, idempotencyRepository, unitOfWork, fixedClock);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private RequestAvailabilityCommand buildCommand(boolean connected, boolean available) {
        return new RequestAvailabilityCommand(
                DRIVER_ID, VEHICLE_ID, connected, available,
                IDEM_KEY, REQUEST_HASH, PRINCIPAL, CORRELATION_ID);
    }

    /** Makes UnitOfWork execute the supplier inline (simulating a transaction). */
    @SuppressWarnings("unchecked")
    private void mockTransactionExecutesInline() {
        when(unitOfWork.executeInTransaction(any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private AvailabilityIntent buildIntent() {
        return AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, FIXED_NOW);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Idempotency
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("replays response when same key+hash found")
        void replayOnSameKeyAndHash() {
            IdempotencyRecord record = new IdempotencyRecord(
                    PRINCIPAL, "PUT",
                    "/api/v1/drivers/" + DRIVER_ID + "/availability-intent",
                    IDEM_KEY, REQUEST_HASH, 202, null);
            when(idempotencyRepository.findByKey(any(), any(), any(), any()))
                    .thenReturn(Optional.of(record));

            AvailabilityIntent intent = buildIntent();
            when(intentRepository.findByDriverId(DRIVER_ID)).thenReturn(Optional.of(intent));

            RequestAvailabilityResult result = service.execute(buildCommand(true, true));

            assertThat(result.fromCache()).isTrue();
            assertThat(result.driverId()).isEqualTo(DRIVER_ID);
            // No driver validation or transaction should occur
            verifyNoInteractions(driverRepository, unitOfWork);
        }

        @Test
        @DisplayName("throws IdempotencyConflictException when same key with different hash")
        void conflictOnDifferentHash() {
            IdempotencyRecord record = new IdempotencyRecord(
                    PRINCIPAL, "PUT",
                    "/api/v1/drivers/" + DRIVER_ID + "/availability-intent",
                    IDEM_KEY, "different-hash", 202, null);
            when(idempotencyRepository.findByKey(any(), any(), any(), any()))
                    .thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.execute(buildCommand(true, true)))
                    .isInstanceOf(IdempotencyConflictException.class)
                    .hasMessageContaining(IDEM_KEY);

            verifyNoInteractions(driverRepository, unitOfWork);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Driver validation
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Driver validation")
    class DriverValidationTests {

        @BeforeEach
        void noIdempotencyRecord() {
            when(idempotencyRepository.findByKey(any(), any(), any(), any()))
                    .thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("throws DriverNotFoundException when driver does not exist")
        void driverNotFound() {
            when(driverRepository.existsById(DRIVER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(buildCommand(true, true)))
                    .isInstanceOf(DriverNotFoundException.class);

            verifyNoInteractions(unitOfWork);
        }

        @Test
        @DisplayName("throws DriverNotQualifiedException when driver is not APPROVED")
        void driverNotApproved() {
            when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
            when(driverRepository.isApproved(DRIVER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(buildCommand(true, true)))
                    .isInstanceOf(DriverNotQualifiedException.class)
                    .hasMessageContaining("APPROVED");

            verifyNoInteractions(unitOfWork);
        }

        @Test
        @DisplayName("throws DriverNotQualifiedException when vehicle not owned/approved")
        void vehicleNotOwned() {
            when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
            when(driverRepository.isApproved(DRIVER_ID)).thenReturn(true);
            when(driverRepository.hasOwnQualifiedVehicle(DRIVER_ID, VEHICLE_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(buildCommand(true, true)))
                    .isInstanceOf(DriverNotQualifiedException.class)
                    .hasMessageContaining("Vehicle");

            verifyNoInteractions(unitOfWork);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Happy path — create
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPathTests {

        @BeforeEach
        void setupDriverValidation() {
            when(idempotencyRepository.findByKey(any(), any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(driverRepository.existsById(DRIVER_ID)).thenReturn(true);
            mockTransactionExecutesInline();
        }

        @Test
        @DisplayName("creates new intent in PENDING status with version=1")
        void createNewIntent() {
            when(driverRepository.isApproved(DRIVER_ID)).thenReturn(true);
            when(driverRepository.hasOwnQualifiedVehicle(DRIVER_ID, VEHICLE_ID)).thenReturn(true);
            when(intentRepository.findByDriverId(DRIVER_ID)).thenReturn(Optional.empty());
            AvailabilityIntent saved = buildIntent();
            when(intentRepository.save(any())).thenReturn(saved);

            RequestAvailabilityResult result = service.execute(buildCommand(true, true));

            assertThat(result.fromCache()).isFalse();
            assertThat(result.status()).isEqualTo(IntentStatus.PENDING);
            assertThat(result.connected()).isTrue();
            assertThat(result.available()).isTrue();
            assertThat(result.desiredVersion()).isEqualTo(1L);

            // Verify all four ports were called inside transaction
            verify(intentRepository).save(any());
            verify(auditLogRepository).append(eq("AvailabilityIntent"), any(), eq("CREATE_INTENT"),
                    eq(PRINCIPAL), eq(CORRELATION_ID), anyString());
            verify(outboxRepository).enqueue(any(), eq(CORRELATION_ID));
            verify(idempotencyRepository).save(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("updates existing intent and increments desiredVersion")
        void updateExistingIntent() {
            when(driverRepository.isApproved(DRIVER_ID)).thenReturn(true);
            when(driverRepository.hasOwnQualifiedVehicle(DRIVER_ID, VEHICLE_ID)).thenReturn(true);
            AvailabilityIntent existing = buildIntent();
            when(intentRepository.findByDriverId(DRIVER_ID)).thenReturn(Optional.of(existing));
            when(intentRepository.save(any())).thenReturn(existing);

            RequestAvailabilityCommand cmd = new RequestAvailabilityCommand(
                    DRIVER_ID, VEHICLE_ID, true, false,
                    IDEM_KEY, REQUEST_HASH, PRINCIPAL, CORRELATION_ID);

            RequestAvailabilityResult result = service.execute(cmd);

            assertThat(result.fromCache()).isFalse();
            assertThat(result.desiredVersion()).isEqualTo(2L);
            assertThat(result.connected()).isTrue();
            assertThat(result.available()).isFalse();

            verify(auditLogRepository).append(eq("AvailabilityIntent"), any(),
                    eq("UPDATE_INTENT"), any(), any(), anyString());
        }

        @Test
        @DisplayName("result status is always PENDING after use case execution")
        void resultStatusIsPending() {
            when(driverRepository.isApproved(DRIVER_ID)).thenReturn(true);
            when(driverRepository.hasOwnQualifiedVehicle(DRIVER_ID, VEHICLE_ID)).thenReturn(true);
            when(intentRepository.findByDriverId(DRIVER_ID)).thenReturn(Optional.empty());
            AvailabilityIntent saved = buildIntent();
            when(intentRepository.save(any())).thenReturn(saved);

            RequestAvailabilityResult result = service.execute(buildCommand(true, true));

            assertThat(result.status()).isEqualTo(IntentStatus.PENDING);
        }

        @Test
        @DisplayName("persists connected=false, available=false without error")
        void disconnected_persistsCorrectly() {
            when(intentRepository.findByDriverId(DRIVER_ID)).thenReturn(Optional.empty());
            AvailabilityIntent saved =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, false, false, FIXED_NOW);
            when(intentRepository.save(any())).thenReturn(saved);

            RequestAvailabilityResult result = service.execute(buildCommand(false, false));

            assertThat(result.connected()).isFalse();
            assertThat(result.available()).isFalse();
            verify(driverRepository,never()).isApproved(any());
        }
    }
}
