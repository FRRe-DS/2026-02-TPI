package ar.edu.utn.frre.m3.application.location;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Location payload sent to M4; M3 suppresses availability while a driver has an assigned trip. */
public record LocationUpdate(
        UUID driverId,
        UUID vehicleId,
        boolean enabled,
        String vehicleType,
        boolean connected,
        boolean available,
        double latitude,
        double longitude,
        OffsetDateTime recordedAt) {
}
