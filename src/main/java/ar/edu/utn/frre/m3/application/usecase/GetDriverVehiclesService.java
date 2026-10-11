package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementación de la lógica de negocio para consultar vehículos (HU-04).
 */
public class GetDriverVehiclesService implements GetDriverVehiclesUseCase {

    private final VehicleRepository vehicleRepository;

    public GetDriverVehiclesService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository cannot be null");
    }

    @Override
    public List<Vehicle> listVehiclesByDriver(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId cannot be null");
        return vehicleRepository.findByDriverId(driverId);
    }

    @Override
    public Vehicle getVehicle(UUID driverId, UUID vehicleId) {
        Objects.requireNonNull(driverId, "driverId cannot be null");
        Objects.requireNonNull(vehicleId, "vehicleId cannot be null");

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        if (!vehicle.belongsToDriver(driverId)) {
            throw new VehicleForbiddenException(driverId, vehicleId);
        }

        return vehicle;
    }
}