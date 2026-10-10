package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {
    Optional<Vehicle> findById(UUID vehicleId);
    boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId);
}
