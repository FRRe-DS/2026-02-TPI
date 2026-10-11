package ar.edu.utn.frre.m3.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Vehicle domain model used for ownership and existence validation.
 * Per HU-06 boundaries, this model is read-only and does not handle vehicle mutations.
 */
public class Vehicle {

    private final UUID id;
    private final UUID driverId;

    public Vehicle(UUID id, UUID driverId) {
        this.id = Objects.requireNonNull(id, "vehicle id cannot be null");
        this.driverId = Objects.requireNonNull(driverId, "driver id cannot be null");
    }

    public UUID getId() {
        return id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    /**
     * Verifies if the vehicle belongs to the given driver.
     */
    public boolean belongsToDriver(UUID candidateDriverId) {
        return this.driverId.equals(candidateDriverId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

