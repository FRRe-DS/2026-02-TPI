package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.Vehicle;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para la consulta de vehículos de un conductor (HU-04 / RF-3.2).
 */
public interface GetDriverVehiclesUseCase {

    List<Vehicle> listVehiclesByDriver(UUID driverId);

    Vehicle getVehicle(UUID driverId, UUID vehicleId);
}