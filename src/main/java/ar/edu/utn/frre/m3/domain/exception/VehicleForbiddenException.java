package ar.edu.utn.frre.m3.domain.exception;

import java.util.UUID;

/**
 * Thrown when the vehicle exists but does not belong to the requested driver (403 Forbidden).
 */
public class VehicleForbiddenException extends RuntimeException {

    private final UUID driverId;
    private final UUID vehicleId;

    public VehicleForbiddenException(UUID driverId, UUID vehicleId) {
        super(String.format("Vehicle %s does not belong to driver %s", vehicleId, driverId));
        this.driverId = driverId;
        this.vehicleId = vehicleId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }
}

