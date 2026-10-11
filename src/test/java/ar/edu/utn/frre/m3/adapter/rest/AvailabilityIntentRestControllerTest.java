package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityCommand;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityResult;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityUseCase;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.IntentStatus;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotQualifiedException;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityIntentRestController — HU-10 REST adapter tests")
class AvailabilityIntentRestControllerTest {

    @Mock
    private RequestAvailabilityUseCase requestAvailabilityUseCase;

    @Mock
    private AvailabilityIntentRepository intentRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AvailabilityIntentRestController controller =
                new AvailabilityIntentRestController(requestAvailabilityUseCase, intentRepository);
        GlobalRestExceptionHandler exceptionHandler = new GlobalRestExceptionHandler();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("PUT /availability-intent returns 202 Accepted when request is valid")
    void shouldReturn202AcceptedOnValidIntent() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID intentId = UUID.randomUUID();
        Instant now = Instant.now();

        RequestAvailabilityResult result = new RequestAvailabilityResult(
                intentId,
                driverId,
                vehicleId,
                true,
                true,
                IntentStatus.PENDING,
                1L,
                now,
                false
        );

        when(requestAvailabilityUseCase.execute(any(RequestAvailabilityCommand.class)))
                .thenReturn(result);

        String requestBody = String.format("""
                {
                    "vehicleId": "%s",
                    "connected": true,
                    "available": true
                }
                """, vehicleId);

        mockMvc.perform(put("/api/v1/drivers/{driverId}/availability-intent", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idemp-key-123")
                        .header("X-Correlation-ID", "corr-test-123")
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Correlation-ID", "corr-test-123"))
                .andExpect(jsonPath("$.intentId", is(intentId.toString())))
                .andExpect(jsonPath("$.driverId", is(driverId.toString())))
                .andExpect(jsonPath("$.vehicleId", is(vehicleId.toString())))
                .andExpect(jsonPath("$.connected", is(true)))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.desiredVersion", is(1)));
    }

    @Test
    @DisplayName("PUT /availability-intent returns 404 ProblemDetail when driver is not found")
    void shouldReturn404ProblemDetailWhenDriverNotFound() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(requestAvailabilityUseCase.execute(any(RequestAvailabilityCommand.class)))
                .thenThrow(new DriverNotFoundException(driverId));

        String requestBody = String.format("""
                {
                    "vehicleId": "%s",
                    "connected": true,
                    "available": true
                }
                """, vehicleId);

        mockMvc.perform(put("/api/v1/drivers/{driverId}/availability-intent", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idemp-key-404")
                        .header("X-Correlation-ID", "corr-404")
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Driver Not Found")))
                .andExpect(jsonPath("$.code", is("DRIVER_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", is("corr-404")));
    }

    @Test
    @DisplayName("PUT /availability-intent returns 422 ProblemDetail when driver is not qualified")
    void shouldReturn422ProblemDetailWhenDriverNotQualified() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(requestAvailabilityUseCase.execute(any(RequestAvailabilityCommand.class)))
                .thenThrow(new DriverNotQualifiedException(driverId, "Driver has no approved vehicle"));

        String requestBody = String.format("""
                {
                    "vehicleId": "%s",
                    "connected": true,
                    "available": true
                }
                """, vehicleId);

        mockMvc.perform(put("/api/v1/drivers/{driverId}/availability-intent", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idemp-key-422")
                        .header("X-Correlation-ID", "corr-422")
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.title", is("Driver Not Qualified")))
                .andExpect(jsonPath("$.code", is("DRIVER_NOT_QUALIFIED")))
                .andExpect(jsonPath("$.reason", is("Driver has no approved vehicle")))
                .andExpect(jsonPath("$.correlationId", is("corr-422")));
    }

    @Test
    @DisplayName("PUT /availability-intent returns 409 ProblemDetail on idempotency conflict")
    void shouldReturn409ProblemDetailOnIdempotencyConflict() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(requestAvailabilityUseCase.execute(any(RequestAvailabilityCommand.class)))
                .thenThrow(new IdempotencyConflictException("idemp-conflict-key"));

        String requestBody = String.format("""
                {
                    "vehicleId": "%s",
                    "connected": true,
                    "available": true
                }
                """, vehicleId);

        mockMvc.perform(put("/api/v1/drivers/{driverId}/availability-intent", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "idemp-conflict-key")
                        .header("X-Correlation-ID", "corr-409")
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.title", is("Idempotency Key Conflict")))
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_CONFLICT")))
                .andExpect(jsonPath("$.correlationId", is("corr-409")));
    }

    @Test
    @DisplayName("GET /availability returns 200 OK when availability intent exists")
    void shouldReturn200WithCurrentAvailabilityState() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant now = Instant.now();

        AvailabilityIntent intent = AvailabilityIntent.create(
                driverId,
                vehicleId,
                true,
                true,
                now
        );

        when(intentRepository.findByDriverId(driverId)).thenReturn(Optional.of(intent));

        mockMvc.perform(get("/api/v1/drivers/{driverId}/availability", driverId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intentId", is(intent.getId().toString())))
                .andExpect(jsonPath("$.driverId", is(driverId.toString())))
                .andExpect(jsonPath("$.vehicleId", is(vehicleId.toString())))
                .andExpect(jsonPath("$.connected", is(true)))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.desiredVersion", is(1)));
    }

    @Test
    @DisplayName("GET /availability returns 404 ProblemDetail when no intent exists")
    void shouldReturn404WhenNoIntentExists() throws Exception {
        UUID driverId = UUID.randomUUID();

        when(intentRepository.findByDriverId(driverId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/drivers/{driverId}/availability", driverId)
                        .header("X-Correlation-ID", "corr-get-404")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("DRIVER_NOT_FOUND")))
                .andExpect(jsonPath("$.correlationId", is("corr-get-404")));
    }
}
