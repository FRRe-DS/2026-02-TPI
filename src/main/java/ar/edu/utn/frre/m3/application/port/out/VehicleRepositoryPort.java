package ar.edu.utn.frre.m3.application.port.out;

import java.util.UUID;

public interface VehicleRepositoryPort {
    boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId);
}
