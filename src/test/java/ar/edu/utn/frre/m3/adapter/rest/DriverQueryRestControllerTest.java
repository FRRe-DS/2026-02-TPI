package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverQueryRestController.class)
@AutoConfigureMockMvc(addFilters = false) // Desactiva filtros de seguridad para aislar el controlador REST
@DisplayName("Pruebas MockMvc de Adaptador REST: DriverQueryRestController (HU-02)")
class DriverQueryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetDriverProfileUseCase getDriverProfileUseCase;

    @MockBean
    private GetMyDriverProfileUseCase getMyDriverProfileUseCase;

    private Driver sampleDriver;
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PersonalInfo info = new PersonalInfo(
                "Lautaro",
                "González",
                "42158963",
                LocalDate.of(1999, 8, 20),
                "Av. Castelli 950",
                "+54 362 4558899",
                "lautaro.gonzalez@example.com"
        );
        sampleDriver = new Driver(driverId, "keycloak-sub-123", info, Instant.now());
    }

    @Test
    @DisplayName("GET /api/v1/me/driver debe retornar 200 OK con ETag y perfil en JSON")
    void shouldReturnProfileForCurrentDriver() throws Exception {
        when(getMyDriverProfileUseCase.getMyDriverProfile()).thenReturn(sampleDriver);

        mockMvc.perform(get("/api/v1/me/driver")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.id").value(driverId.toString()))
                .andExpect(jsonPath("$.identitySubject").value("keycloak-sub-123"))
                .andExpect(jsonPath("$.personalInfo.firstName").value("Lautaro"))
                .andExpect(jsonPath("$.personalInfo.lastName").value("González"))
                .andExpect(jsonPath("$.personalInfo.dni").value("42158963"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/drivers/{driverId} debe retornar 200 OK con perfil por ID")
    void shouldReturnProfileById() throws Exception {
        when(getDriverProfileUseCase.getDriverProfileById(driverId)).thenReturn(sampleDriver);

        mockMvc.perform(get("/api/v1/drivers/{driverId}", driverId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.id").value(driverId.toString()))
                .andExpect(jsonPath("$.personalInfo.fullName").value("Lautaro González"));
    }

    @Test
    @DisplayName("GET /api/v1/drivers/{driverId} debe retornar 404 ProblemDetail cuando no existe")
    void shouldReturn404ProblemDetailWhenNotFound() throws Exception {
        when(getDriverProfileUseCase.getDriverProfileById(driverId))
                .thenThrow(DriverNotFoundException.forId(driverId));

        mockMvc.perform(get("/api/v1/drivers/{driverId}", driverId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DRIVER_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("Conductor no encontrado"));
    }

    @Test
    @DisplayName("GET /api/v1/drivers/{driverId} debe retornar 403 ProblemDetail cuando no tiene permisos")
    void shouldReturn403ProblemDetailWhenAccessDenied() throws Exception {
        when(getDriverProfileUseCase.getDriverProfileById(driverId))
                .thenThrow(new DriverAccessDeniedException("No tiene permisos para consultar este perfil."));

        mockMvc.perform(get("/api/v1/drivers/{driverId}", driverId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    @DisplayName("GET /api/v1/drivers/uuid-invalido debe retornar 400 ProblemDetail")
    void shouldReturn400ProblemDetailForInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/uuid-invalido")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_FORMAT"));
    }
}
