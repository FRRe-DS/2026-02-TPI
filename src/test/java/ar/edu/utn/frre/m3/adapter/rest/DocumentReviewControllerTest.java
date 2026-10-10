package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.ReviewDocumentRequest;
import ar.edu.utn.frre.m3.adapter.rest.mapper.DocumentRestMapper;
import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentUseCase;
import ar.edu.utn.frre.m3.application.port.out.IdempotencyRepositoryPort;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import ar.edu.utn.frre.m3.domain.model.Document;
import ar.edu.utn.frre.m3.domain.model.DocumentKind;
import ar.edu.utn.frre.m3.domain.model.IdempotencyRecord;
import ar.edu.utn.frre.m3.domain.model.ReviewDecision;
import ar.edu.utn.frre.m3.domain.model.ReviewStatus;
import ar.edu.utn.frre.m3.domain.model.ServiceType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentReviewController.class)
@Import({GlobalExceptionHandler.class, DocumentRestMapper.class})
class DocumentReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewDocumentUseCase reviewDocumentUseCase;

    @MockBean
    private IdempotencyRepositoryPort idempotencyRepositoryPort;

    private UUID driverId;
    private UUID documentId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        idempotencyKey = "idemp-key-test-123456789";
    }

    @Test
    @DisplayName("200 OK: Revisión exitosa con ETag y X-Correlation-ID en la respuesta")
    void shouldReturn200WithDocumentAndETagWhenReviewIsSuccessful() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED,
                "Documentación verificada con éxito"
        );

        Document reviewedDocument = new Document(
                documentId,
                driverId,
                null,
                DocumentKind.LICENSE,
                1,
                ReviewStatus.APPROVED,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(36000),
                true,
                List.of(ServiceType.AUTO),
                8L,
                Instant.now().minusSeconds(7200)
        );

        when(idempotencyRepositoryPort.findByKeyAndMethodAndPath(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class)))
                .thenReturn(reviewedDocument);

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-test-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"8\""))
                .andExpect(header().string("X-Correlation-ID", "corr-test-123"))
                .andExpect(jsonPath("$.id", is(documentId.toString())))
                .andExpect(jsonPath("$.driverId", is(driverId.toString())))
                .andExpect(jsonPath("$.reviewStatus", is("APPROVED")))
                .andExpect(jsonPath("$.driverVersion", is(8)));

        verify(idempotencyRepositoryPort).save(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("428 Precondition Required: Cuando falta el header If-Match")
    void shouldReturn428WhenIfMatchHeaderIsMissing() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED,
                "Verificación"
        );

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-missing-ifmatch")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionRequired())
                .andExpect(header().string("X-Correlation-ID", "corr-missing-ifmatch"))
                .andExpect(jsonPath("$.type", is("urn:m3:problem:precondition-required")))
                .andExpect(jsonPath("$.status", is(428)))
                .andExpect(jsonPath("$.code", is("PRECONDITION_REQUIRED")));
    }

    @Test
    @DisplayName("412 Precondition Failed: Cuando la versión no coincide con la esperada")
    void shouldReturn412WhenVersionMismatchOccurs() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED,
                "Verificación"
        );

        when(idempotencyRepositoryPort.findByKeyAndMethodAndPath(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class)))
                .thenThrow(new VersionMismatchException(8L, 7L));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-412-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(header().string("X-Correlation-ID", "corr-412-test"))
                .andExpect(jsonPath("$.type", is("urn:m3:problem:precondition-failed")))
                .andExpect(jsonPath("$.status", is(412)))
                .andExpect(jsonPath("$.code", is("PRECONDITION_FAILED")));
    }

    @Test
    @DisplayName("404 Not Found: Cuando el documento o conductor no existe")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED,
                "Verificación"
        );

        when(idempotencyRepositoryPort.findByKeyAndMethodAndPath(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class)))
                .thenThrow(new DocumentNotFoundException(documentId));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-404-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-ID", "corr-404-test"))
                .andExpect(jsonPath("$.type", is("urn:m3:problem:not-found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("400 Bad Request: Cuando falta Idempotency-Key o es menor a 16 caracteres")
    void shouldReturn400WhenIdempotencyKeyIsTooShort() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED,
                "Verificación"
        );

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", "too-short")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:bad-request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", containsString("Idempotency-Key must be between 16 and 128 characters")));
    }

    @Test
    @DisplayName("409 Conflict: Cuando la clave idempotente se reutiliza con distinto payload")
    void shouldReturn409WhenIdempotencyKeyIsReusedWithDifferentPayload() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.REJECTED,
                "Otro motivo distinto"
        );

        IdempotencyRecord previousRecord = new IdempotencyRecord(
                UUID.randomUUID(),
                idempotencyKey,
                "OPERATOR",
                "POST",
                "/api/v1/drivers/" + driverId + "/documents/" + documentId + "/reviews",
                "previous-hash-123456",
                200,
                "{}",
                Instant.now()
        );

        when(idempotencyRepositoryPort.findByKeyAndMethodAndPath(any(), any(), any()))
                .thenReturn(Optional.of(previousRecord));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-409-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string("X-Correlation-ID", "corr-409-test"))
                .andExpect(jsonPath("$.type", is("urn:m3:problem:conflict")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD")));
    }
}
