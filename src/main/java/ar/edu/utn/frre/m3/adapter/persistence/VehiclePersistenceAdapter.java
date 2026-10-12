package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.mapper.VehicleMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia que implementa el puerto VehicleRepository.
 */
@Repository
public class VehiclePersistenceAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository repository;
    private final VehicleMapper mapper;

    public VehiclePersistenceAdapter(SpringDataVehicleRepository repository, VehicleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Vehicle save(Vehicle v) {
        var e = new ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity();
        e.setColor(v.getColor()); e.setCapacity(v.getCapacity());
        e.setServiceCategories(String.join(",", new java.util.TreeSet<>(v.getServiceCategories())));
        e.setId(v.getId()); e.setDriverId(v.getDriverId()); e.setVehicleType(v.getVehicleType());
        e.setServiceTypes(String.join(",", v.getServiceTypes())); e.setCountryCode(v.getCountryCode());
        e.setPlate(v.getPlate()); e.setMake(v.getMake()); e.setModel(v.getModel());
        e.setModelYear(v.getModelYear()); e.setQualificationStatus(v.getQualificationStatus());
        e.setDriverVersion(v.getDriverVersion()); e.setCreatedAt(v.getCreatedAt()); e.setUpdatedAt(v.getCreatedAt());
        return mapper.toDomain(repository.save(e));
    }

    @Override
    public Optional<Vehicle> findById(UUID vehicleId) {
        return repository.findById(vehicleId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId) {
        return repository.existsByIdAndDriverId(vehicleId, driverId);
    }

    @Override
    public List<Vehicle> findByDriverId(UUID driverId) {
        return repository.findByDriverIdOrderByCreatedAtDesc(driverId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Vehicle> findByIdAndDriverId(UUID vehicleId, UUID driverId) {
        return repository.findByIdAndDriverId(vehicleId, driverId)
            .map(mapper::toDomain);
    }
}