package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.DriverNotQualifiedException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link AvailabilityIntent} domain entity — HU-10.
 *
 * <p>Tests validate the following business rules:
 * <ol>
 *   <li>connected=false must imply available=false.</li>
 *   <li>Factory method creates intent in PENDING status, version=1.</li>
 *   <li>update() increments desiredVersion and resets to PENDING.</li>
 *   <li>markApplied() / markRetrying() / markFailed() transition status correctly.</li>
 *   <li>IllegalArgumentException on connected=false & available=true.</li>
 * </ol>
 */
@DisplayName("AvailabilityIntent — domain rules")
class AvailabilityIntentTest {

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final UUID VEHICLE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-10T21:00:00Z");

    // ──────────────────────────────────────────────────────────────────────────
    // Factory / Creation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("creates a PENDING intent with version=1 when connected and available")
        void create_connectedAndAvailable_returnsPending() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);

            assertThat(intent.getId()).isNotNull();
            assertThat(intent.getDriverId()).isEqualTo(DRIVER_ID);
            assertThat(intent.getVehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(intent.isConnected()).isTrue();
            assertThat(intent.isAvailable()).isTrue();
            assertThat(intent.getStatus()).isEqualTo(IntentStatus.PENDING);
            assertThat(intent.getDesiredVersion()).isEqualTo(1L);
            assertThat(intent.getAppliedVersion()).isNull();
            assertThat(intent.getCreatedAt()).isEqualTo(NOW);
            assertThat(intent.getUpdatedAt()).isEqualTo(NOW);
            assertThat(intent.getSyncedAt()).isNull();
        }

        @Test
        @DisplayName("creates a PENDING intent when connected=true, available=false")
        void create_connectedNotAvailable_returnsPending() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, false, NOW);

            assertThat(intent.isConnected()).isTrue();
            assertThat(intent.isAvailable()).isFalse();
            assertThat(intent.getStatus()).isEqualTo(IntentStatus.PENDING);
        }

        @Test
        @DisplayName("creates a PENDING intent when connected=false, available=false (disconnected)")
        void create_disconnected_returnsPending() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, false, false, NOW);

            assertThat(intent.isConnected()).isFalse();
            assertThat(intent.isAvailable()).isFalse();
            assertThat(intent.getStatus()).isEqualTo(IntentStatus.PENDING);
        }

        @Test
        @DisplayName("rejects connected=false with available=true (business rule violation)")
        void create_connectedFalseAvailableTrue_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, false, true, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available cannot be true when connected is false");
        }

        @Test
        @DisplayName("rejects null driverId")
        void create_nullDriverId_throwsNullPointer() {
            assertThatThrownBy(() ->
                AvailabilityIntent.create(null, VEHICLE_ID, true, false, NOW))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null vehicleId")
        void create_nullVehicleId_throwsNullPointer() {
            assertThatThrownBy(() ->
                AvailabilityIntent.create(DRIVER_ID, null, true, false, NOW))
                .isInstanceOf(NullPointerException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // update()
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("increments desiredVersion and resets status to PENDING")
        void update_incrementsVersion() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);
            intent.markApplied(1L, NOW); // simulate applied

            Instant later = NOW.plusSeconds(60);
            intent.update(VEHICLE_ID, true, false, later);

            assertThat(intent.getDesiredVersion()).isEqualTo(2L);
            assertThat(intent.getStatus()).isEqualTo(IntentStatus.PENDING);
            assertThat(intent.isConnected()).isTrue();
            assertThat(intent.isAvailable()).isFalse();
            assertThat(intent.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("rejects connected=false with available=true in update")
        void update_invalidState_throws() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);

            assertThatThrownBy(() ->
                intent.update(VEHICLE_ID, false, true, NOW.plusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("can change vehicle during update")
        void update_changesVehicle() {
            UUID newVehicle = UUID.randomUUID();
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, false, NOW);
            intent.update(newVehicle, true, true, NOW.plusSeconds(5));

            assertThat(intent.getVehicleId()).isEqualTo(newVehicle);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Status transitions
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("markApplied sets status=APPLIED and records appliedVersion and syncedAt")
        void markApplied_setsStatus() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);
            Instant syncTime = NOW.plusSeconds(5);
            intent.markApplied(1L, syncTime);

            assertThat(intent.getStatus()).isEqualTo(IntentStatus.APPLIED);
            assertThat(intent.getAppliedVersion()).isEqualTo(1L);
            assertThat(intent.getSyncedAt()).isEqualTo(syncTime);
            assertThat(intent.getUpdatedAt()).isEqualTo(syncTime);
        }

        @Test
        @DisplayName("markRetrying sets status=RETRYING")
        void markRetrying_setsStatus() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);
            intent.markRetrying(NOW.plusSeconds(2));

            assertThat(intent.getStatus()).isEqualTo(IntentStatus.RETRYING);
        }

        @Test
        @DisplayName("markFailed sets status=FAILED")
        void markFailed_setsStatus() {
            AvailabilityIntent intent =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);
            intent.markFailed(NOW.plusSeconds(120));

            assertThat(intent.getStatus()).isEqualTo(IntentStatus.FAILED);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Equality
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("equals and hashCode")
    class EqualityTests {

        @Test
        @DisplayName("two intents with the same UUID are equal")
        void sameId_areEqual() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            AvailabilityIntent a = new AvailabilityIntent(
                id, DRIVER_ID, VEHICLE_ID, true, false, IntentStatus.PENDING, 1, null, now, now, null);
            AvailabilityIntent b = new AvailabilityIntent(
                id, UUID.randomUUID(), UUID.randomUUID(), false, false, IntentStatus.FAILED, 5, 3L, now, now, null);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("two intents with different UUIDs are not equal")
        void differentId_notEqual() {
            AvailabilityIntent a =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);
            AvailabilityIntent b =
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, true, true, NOW);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
