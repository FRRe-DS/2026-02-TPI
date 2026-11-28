package ar.edu.utn.frre.m3.application.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lo que M3 necesita saber de un viaje según M6 ({@code GET /api/v1/trips/{tripId}}, contrato M6 1.0.0/2.0.0
 * de los grupos 05 y 12). {@code state} es el estado de M6: PENDING_ARRIVAL, DRIVER_ARRIVED, IN_PROGRESS,
 * COMPLETED o CANCELLED. Las coordenadas finales sólo existen si el viaje terminó y M6 las registró.
 */
public record M6TripState(String tripId, UUID driverId, String clientId, String state,
        Double finalLatitude, Double finalLongitude, Double destinationLatitude, Double destinationLongitude,
        OffsetDateTime completedAt) {

    public boolean isCompleted() { return "COMPLETED".equals(state); }
    public boolean isCancelled() { return "CANCELLED".equals(state); }
    public boolean isInProgress() { return "IN_PROGRESS".equals(state); }
}
