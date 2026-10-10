package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDriverVehiclesServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    private GetDriverVehiclesService service;

    @BeforeEach
    void setUp() {
        service = new GetDriverVehiclesService(vehicleRepository);
    }

    @Test
    @DisplayName("Debe listar los vehículos pertenecientes al conductor")
    void shouldListVehiclesSuccessfully() {
        UUID driverId = UUID.randomUUID();
        Vehicle v = new Vehicle(UUID.randomUUID(), driverId);
        when(vehicleRepository.findByDriverId(driverId)).thenReturn(List.of(v));

        List<Vehicle> result = service.listVehiclesByDriver(driverId);
        assertEquals(1, result.size());
        assertEquals(driverId, result.get(0).getDriverId());
    }

    @Test
    @DisplayName("Debe lanzar VehicleNotFoundException (404) cuando el vehículo no existe")
    void shouldThrow404WhenVehicleNotFound() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> service.getVehicle(driverId, vehicleId));
    }

    @Test
    @DisplayName("Debe lanzar VehicleForbiddenException (403) cuando el vehículo pertenece a otro conductor")
    void shouldThrow403WhenVehicleBelongsToOtherDriver() {
        UUID driverId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle v = new Vehicle(vehicleId, otherDriverId);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(v));

        assertThrows(VehicleForbiddenException.class, () -> service.getVehicle(driverId, vehicleId));
    }
}