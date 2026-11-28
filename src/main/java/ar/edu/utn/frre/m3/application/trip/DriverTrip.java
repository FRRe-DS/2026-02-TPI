package ar.edu.utn.frre.m3.application.trip;

import ar.edu.utn.frre.m3.application.location.GeoPosition;
import java.time.OffsetDateTime;

public record DriverTrip(String tripId, DriverOffer offer, TripStatus status,
        OffsetDateTime startedAt, OffsetDateTime completedAt, GeoPosition destination, Integer driverRating) {
    public DriverTrip withStatus(TripStatus newStatus, OffsetDateTime startedAt) {
        return new DriverTrip(tripId, offer, newStatus, startedAt, completedAt, destination, driverRating);
    }

    public DriverTrip completed(OffsetDateTime completedAt, GeoPosition destination, Integer driverRating) {
        return new DriverTrip(tripId, offer, TripStatus.FINALIZADO, startedAt, completedAt, destination, driverRating);
    }

    public DriverTrip cancelled(OffsetDateTime cancelledAt) {
        return new DriverTrip(tripId, offer, TripStatus.CANCELADO, startedAt, cancelledAt, destination, driverRating);
    }

    public boolean isActive() {
        return status == TripStatus.ASSIGNED || status == TripStatus.IN_PROGRESS;
    }

    public enum TripStatus { ASSIGNED, IN_PROGRESS, FINALIZADO, CANCELADO }
}
