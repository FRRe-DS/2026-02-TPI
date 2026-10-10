package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.mapper.VehicleMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class VehiclePersistenceAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository repository;
    private final VehicleMapper mapper;

    public VehiclePersistenceAdapter(SpringDataVehicleRepository repository, VehicleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Vehicle> findById(UUID vehicleId) {
        return repository.findById(vehicleId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId) {
        return repository.existsByIdAndDriverId(vehicleId, driverId);
    }
}
