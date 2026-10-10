package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentCommand;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentUseCase;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentRestControllerTest {

    @Mock
    private RegisterVehicleDocumentUseCase registerVehicleDocumentUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        VehicleDocumentRestController controller = new VehicleDocumentRestController(registerVehicleDocumentUseCase);
        GlobalRestExceptionHandler exceptionHandler = new GlobalRestExceptionHandler();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(exceptionHandler)
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /documents should return 201 Created and Location header on success")
    void shouldReturn201CreatedOnSuccess() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().plus(365, ChronoUnit.DAYS);

        Document createdDocument = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-445566",
            "La Segunda",
            issuedAt,
            validUntil,
            "ref-abc",
            1
        );

        when(registerVehicleDocumentUseCase.registerVehicleDocument(any(RegisterVehicleDocumentCommand.class)))
            .thenReturn(createdDocument);

        String requestJson = String.format("""
            {
                "documentType": "INSURANCE",
                "documentNumber": "POL-445566",
                "issuingEntity": "La Segunda",
                "issuedAt": "%s",
                "validUntil": "%s",
                "storageReference": "ref-abc"
            }
            """, issuedAt, validUntil);

        mockMvc.perform(post("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", "corr-test-123")
                .content(requestJson))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", String.format("/api/v1/drivers/%s/vehicles/%s/documents/%s", driverId, vehicleId, createdDocument.getId())))
            .andExpect(jsonPath("$.id", is(createdDocument.getId().toString())))
            .andExpect(jsonPath("$.driverId", is(driverId.toString())))
            .andExpect(jsonPath("$.vehicleId", is(vehicleId.toString())))
            .andExpect(jsonPath("$.documentType", is("INSURANCE")))
            .andExpect(jsonPath("$.kind", is("INSURANCE")))
            .andExpect(jsonPath("$.documentNumber", is("POL-445566")))
            .andExpect(jsonPath("$.issuingEntity", is("La Segunda")))
            .andExpect(jsonPath("$.revision", is(1)))
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andExpect(jsonPath("$.reviewStatus", is("PENDING")))
            .andExpect(jsonPath("$.isCurrent", is(true)));
    }

    @Test
    @DisplayName("POST /documents should return 404 ProblemDetail when vehicle is not found")
    void shouldReturn404ProblemDetailWhenVehicleNotFound() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().plus(30, ChronoUnit.DAYS);

        when(registerVehicleDocumentUseCase.registerVehicleDocument(any(RegisterVehicleDocumentCommand.class)))
            .thenThrow(new VehicleNotFoundException(vehicleId));

        String requestJson = String.format("""
            {
                "documentType": "INSURANCE",
                "documentNumber": "POL-111",
                "issuedAt": "%s",
                "validUntil": "%s"
            }
            """, issuedAt, validUntil);

        mockMvc.perform(post("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", "corr-404")
                .content(requestJson))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status", is(404)))
            .andExpect(jsonPath("$.title", is("Vehicle Not Found")))
            .andExpect(jsonPath("$.code", is("VEHICLE_NOT_FOUND")))
            .andExpect(jsonPath("$.type", is("urn:m3:problem:not-found")))
            .andExpect(jsonPath("$.correlationId", is("corr-404")));
    }

    @Test
    @DisplayName("POST /documents should return 403 ProblemDetail when vehicle belongs to another driver")
    void shouldReturn403ProblemDetailWhenVehicleBelongsToAnotherDriver() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().plus(30, ChronoUnit.DAYS);

        when(registerVehicleDocumentUseCase.registerVehicleDocument(any(RegisterVehicleDocumentCommand.class)))
            .thenThrow(new VehicleForbiddenException(driverId, vehicleId));

        String requestJson = String.format("""
            {
                "documentType": "INSURANCE",
                "documentNumber": "POL-222",
                "issuedAt": "%s",
                "validUntil": "%s"
            }
            """, issuedAt, validUntil);

        mockMvc.perform(post("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", "corr-403")
                .content(requestJson))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.title", is("Vehicle Access Forbidden")))
            .andExpect(jsonPath("$.code", is("VEHICLE_FORBIDDEN")))
            .andExpect(jsonPath("$.type", is("urn:m3:problem:forbidden")))
            .andExpect(jsonPath("$.correlationId", is("corr-403")));
    }

    @Test
    @DisplayName("POST /documents should return 422 ProblemDetail when issuedAt is not before validUntil")
    void shouldReturn422ProblemDetailWhenDatesAreInvalid() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant validUntil = Instant.now();

        when(registerVehicleDocumentUseCase.registerVehicleDocument(any(RegisterVehicleDocumentCommand.class)))
            .thenThrow(new InvalidDocumentPeriodException(issuedAt, validUntil));

        String requestJson = String.format("""
            {
                "documentType": "INSURANCE",
                "documentNumber": "POL-333",
                "issuedAt": "%s",
                "validUntil": "%s"
            }
            """, issuedAt, validUntil);

        mockMvc.perform(post("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-ID", "corr-422")
                .content(requestJson))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status", is(422)))
            .andExpect(jsonPath("$.title", is("Invalid Document Period")))
            .andExpect(jsonPath("$.code", is("INVALID_DOCUMENT_PERIOD")))
            .andExpect(jsonPath("$.type", is("urn:m3:problem:invalid-period")))
            .andExpect(jsonPath("$.correlationId", is("corr-422")));
    }

    @Test
    @DisplayName("POST /documents should return 400 ProblemDetail when mandatory fields are missing")
    void shouldReturn400ProblemDetailWhenValidationFails() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        // Missing documentType, documentNumber, issuedAt, validUntil
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.type", is("urn:m3:problem:validation")))
            .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    @DisplayName("GET /documents should return 200 OK and list of vehicle documents")
    void shouldReturn200AndListOnGet() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Document doc = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-GET",
            "Federacion",
            Instant.now().minus(5, ChronoUnit.DAYS),
            Instant.now().plus(100, ChronoUnit.DAYS),
            null,
            1
        );

        when(registerVehicleDocumentUseCase.listVehicleDocuments(driverId, vehicleId))
            .thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents", driverId, vehicleId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].documentNumber", is("POL-GET")))
            .andExpect(jsonPath("$[0].revision", is(1)));
    }
}

