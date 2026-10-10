package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Vehicle consultation and existence/ownership validation.
 * Mutation operations are out of scope for HU-06.
 */
public interface VehicleRepository {

    Optional<Vehicle> findById(UUID vehicleId);
}

