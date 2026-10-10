package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.mapper.VehicleMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter implementing the VehicleRepository application output port.
 * Exclusively provides read operations for HU-06 validation boundaries.
 */
@Repository
public class VehiclePersistenceAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository springDataVehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehiclePersistenceAdapter(
        SpringDataVehicleRepository springDataVehicleRepository,
        VehicleMapper vehicleMapper
    ) {
        this.springDataVehicleRepository = Objects.requireNonNull(springDataVehicleRepository, "springDataVehicleRepository cannot be null");
        this.vehicleMapper = Objects.requireNonNull(vehicleMapper, "vehicleMapper cannot be null");
    }

    @Override
    public Optional<Vehicle> findById(UUID vehicleId) {
        return springDataVehicleRepository.findById(vehicleId)
            .map(vehicleMapper::toDomain);
    }
}

