package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.IntentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response body for both availability endpoints:
 * <ul>
 *   <li>{@code PUT /api/v1/drivers/{driverId}/availability-intent} → 202 Accepted</li>
 *   <li>{@code GET /api/v1/drivers/{driverId}/availability} → 200 OK</li>
 * </ul>
 *
 * @param intentId       UUID of the intent.
 * @param driverId       Driver that owns this intent.
 * @param vehicleId      Selected vehicle.
 * @param connected      Requested connected state.
 * @param available      Requested available state.
 * @param status         Synchronisation status with M4.
 * @param desiredVersion Version sent to M4.
 * @param updatedAt      Last modification timestamp (ISO 8601).
 */
public record AvailabilityIntentResponse(
        UUID intentId,
        UUID driverId,
        UUID vehicleId,
        boolean connected,
        boolean available,
        IntentStatus status,
        long desiredVersion,
        Instant updatedAt, Long appliedVersion
) {
    public AvailabilityIntentResponse(UUID intentId,UUID driverId,UUID vehicleId,boolean connected,boolean available,
            IntentStatus status,long desiredVersion,Instant updatedAt) {
        this(intentId,driverId,vehicleId,connected,available,status,desiredVersion,updatedAt,null);
    }
}
