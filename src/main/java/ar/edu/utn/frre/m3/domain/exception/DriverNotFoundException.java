package ar.edu.utn.frre.m3.domain.exception;

import java.util.UUID;

public class DriverNotFoundException extends RuntimeException {
    private final UUID driverId;

    public DriverNotFoundException(UUID driverId) {
        super("Driver not found: " + driverId);
        this.driverId = driverId;
    }

    public UUID getDriverId() {
        return driverId;
    }
}
