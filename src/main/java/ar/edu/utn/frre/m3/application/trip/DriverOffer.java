package ar.edu.utn.frre.m3.application.trip;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DriverOffer(UUID offerId, UUID driverId, String requestId, String pickupAddress,
        String destinationAddress, String vehicleType, OffsetDateTime expiresAt, OfferStatus status, String tripId) {
    public DriverOffer withDecision(OfferStatus decision, String assignedTripId) {
        return new DriverOffer(offerId, driverId, requestId, pickupAddress, destinationAddress,
                vehicleType, expiresAt, decision, assignedTripId);
    }

    public enum OfferStatus { PENDING, ACCEPTED, REJECTED, EXPIRED }
}
