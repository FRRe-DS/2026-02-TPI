package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.usecase.GetDriverVehiclesUseCase;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VehicleQueryRestControllerTest {

    @Mock
    private GetDriverVehiclesUseCase getDriverVehiclesUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VehicleQueryRestController controller = new VehicleQueryRestController(getDriverVehiclesUseCase);
        GlobalRestExceptionHandler handler = new GlobalRestExceptionHandler();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler).build();
    }

    @Test
    @DisplayName("GET /vehicles debe devolver 200 OK y la lista de vehículos")
    void shouldReturn200AndVehicleList() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle(vehicleId, driverId);

        when(getDriverVehiclesUseCase.listVehiclesByDriver(driverId)).thenReturn(List.of(vehicle));

        mockMvc.perform(get("/api/v1/drivers/{driverId}/vehicles", driverId)
                .header("X-Correlation-ID", "corr-test"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-ID", "corr-test"))
            .andExpect(jsonPath("$.items", hasSize(1)))
            .andExpect(jsonPath("$.items[0].id", is(vehicleId.toString())));
    }

    @Test
    @DisplayName("GET /vehicles/{id} debe devolver 404 ProblemDetail cuando no existe el vehículo")
    void shouldReturn404WhenVehicleDoesNotExist() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(getDriverVehiclesUseCase.getVehicle(driverId, vehicleId))
            .thenThrow(new VehicleNotFoundException(vehicleId));

        mockMvc.perform(get("/api/v1/drivers/{driverId}/vehicles/{vehicleId}", driverId, vehicleId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /vehicles/{id} debe devolver 403 ProblemDetail cuando el vehículo es de otro conductor")
    void shouldReturn403WhenAccessingVehicleOfAnotherDriver() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(getDriverVehiclesUseCase.getVehicle(driverId, vehicleId))
            .thenThrow(new VehicleForbiddenException(driverId, vehicleId));

        mockMvc.perform(get("/api/v1/drivers/{driverId}/vehicles/{vehicleId}", driverId, vehicleId))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code", is("VEHICLE_FORBIDDEN")));
    }
}