package ar.edu.utn.frre.m3.adapter.persistence.adapter;

import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.out.VehicleRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VehiclePersistenceAdapter implements VehicleRepositoryPort {

    private final SpringDataVehicleRepository vehicleRepository;

    public VehiclePersistenceAdapter(SpringDataVehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public boolean existsByIdAndDriverId(UUID vehicleId, UUID driverId) {
        return vehicleRepository.existsByIdAndDriverId(vehicleId, driverId);
    }
}
