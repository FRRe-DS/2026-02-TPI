package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.mapper.VehicleMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia que implementa el puerto VehicleRepository.
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

    @Override
    public List<Vehicle> findByDriverId(UUID driverId) {
        return springDataVehicleRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
            .stream()
            .map(vehicleMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Vehicle> findByIdAndDriverId(UUID vehicleId, UUID driverId) {
        return springDataVehicleRepository.findByIdAndDriverId(vehicleId, driverId)
            .map(vehicleMapper::toDomain);
    }
}