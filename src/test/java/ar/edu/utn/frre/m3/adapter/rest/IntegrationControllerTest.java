package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.BatchEligibilityRequestDTO;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = IntegrationController.class)
@Import(GlobalRestExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Pruebas MockMvc de Adaptador REST: IntegrationController (HU-09)")
class IntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean ar.edu.utn.frre.m3.application.port.DriverRepository drivers;

    @MockBean
    private CheckDriverAptitudeUseCase checkDriverAptitudeUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/integration/drivers/{id}/eligibility retorna 200 y elegibilidad")
    void getEligibility_returns200() throws Exception {
        UUID driverId = UUID.randomUUID();
        EligibilityResult result = new EligibilityResult(true, List.of(), Instant.now(), 1, Instant.now().plusSeconds(3600));

        when(checkDriverAptitudeUseCase.execute(eq(driverId))).thenReturn(result);

        mockMvc.perform(get("/api/v1/integration/drivers/{id}/eligibility", driverId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/integration/drivers/{id}/eligibility con conductor inexistente retorna 404")
    void getEligibility_driverNotFound_returns404() throws Exception {
        UUID driverId = UUID.randomUUID();
        when(checkDriverAptitudeUseCase.execute(eq(driverId))).thenThrow(DriverNotFoundException.forId(driverId));

        mockMvc.perform(get("/api/v1/integration/drivers/{id}/eligibility", driverId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DRIVER_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/integration/eligibility/batch retorna lista con evaluaciones")
    void batchEligibility_returns200WithResults() throws Exception {
        UUID d1 = UUID.randomUUID();
        UUID d2 = UUID.randomUUID();
        BatchEligibilityRequestDTO request = new BatchEligibilityRequestDTO(List.of(d1, d2));

        EligibilityResult result1 = new EligibilityResult(true, List.of(), Instant.now(), 1, Instant.now().plusSeconds(3600));
        when(checkDriverAptitudeUseCase.execute(eq(d1))).thenReturn(result1);
        when(checkDriverAptitudeUseCase.execute(eq(d2))).thenThrow(DriverNotFoundException.forId(d2));

        mockMvc.perform(post("/api/v1/integration/eligibility/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results[0].driverId").value(d1.toString()))
                .andExpect(jsonPath("$.results[0].eligibility.eligible").value(true))
                .andExpect(jsonPath("$.results[1].driverId").value(d2.toString()))
                .andExpect(jsonPath("$.results[1].eligibility.eligible").value(false))
                .andExpect(jsonPath("$.results[1].eligibility.reasons[0]").value("DRIVER_NOT_FOUND"));
    }
}
