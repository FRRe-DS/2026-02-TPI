package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para consultas y validaciones de vehículos.
 */
public interface VehicleRepository {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(UUID vehicleId);

    List<Vehicle> findByDriverId(UUID driverId);

    Optional<Vehicle> findByIdAndDriverId(UUID vehicleId, UUID driverId);

    boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId);
}