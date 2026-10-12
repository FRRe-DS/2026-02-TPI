package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.IntentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Result returned by the "Request Availability" use case (HU-10).
 *
 * <p>The REST controller uses this to build the 202 Accepted response.
 *
 * @param intentId       UUID of the created/updated intent.
 * @param driverId       Driver that owns the intent.
 * @param vehicleId      Selected vehicle.
 * @param connected      Requested connected state.
 * @param available      Requested available state.
 * @param status         Synchronisation status with M4 (always PENDING after this use case).
 * @param desiredVersion Version sent to M4 for de-duplication.
 * @param updatedAt      Timestamp of the last modification.
 * @param fromCache      True when the response was replayed from an idempotency record.
 */
public record RequestAvailabilityResult(
        UUID intentId,
        UUID driverId,
        UUID vehicleId,
        boolean connected,
        boolean available,
        IntentStatus status,
        long desiredVersion,
        Instant updatedAt,
        boolean fromCache
) {}
