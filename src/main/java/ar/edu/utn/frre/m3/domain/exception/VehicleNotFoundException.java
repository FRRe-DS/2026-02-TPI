package ar.edu.utn.frre.m3.domain.exception;

import java.util.UUID;

/**
 * Thrown when the requested vehicle does not exist.
 */
public class VehicleNotFoundException extends RuntimeException {

    private final UUID vehicleId;

    public VehicleNotFoundException(UUID vehicleId) {
        super(String.format("Vehicle with ID %s not found", vehicleId));
        this.vehicleId = vehicleId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }
}

