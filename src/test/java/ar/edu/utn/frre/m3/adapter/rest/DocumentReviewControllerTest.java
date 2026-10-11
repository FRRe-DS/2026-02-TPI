package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.ReviewDocumentRequest;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.usecase.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.usecase.ReviewDocumentUseCase;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
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
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentReviewController.class)
@Import(GlobalRestExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DocumentReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewDocumentUseCase reviewDocumentUseCase;
    @MockBean private IdempotencyRepository idempotencyRepository;

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
    @DisplayName("200 OK: Revisión exitosa con ETag y X-Correlation-ID")
    void shouldReturn200WithDocumentAndETagWhenReviewIsSuccessful() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(
                ReviewDecision.APPROVED, "Documentación verificada con éxito"
        );

        Document reviewedDocument = new Document(
                documentId, driverId, null, DocumentType.LICENSE, "LIC-123", "DGT",
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(36000),
                null, 1, DocumentStatus.APPROVED, true, 8L, "Documentación verificada con éxito",
                Instant.now(), "OPERATOR", Instant.now().minusSeconds(7200)
        );

        when(idempotencyRepository.findByKeyAndMethodAndPath(any(), any(), any())).thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class))).thenReturn(reviewedDocument);

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

        verify(idempotencyRepository).save(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("428 Precondition Required: Cuando falta el header If-Match")
    void shouldReturn428WhenIfMatchHeaderIsMissing() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(ReviewDecision.APPROVED, "Verificación");

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-missing-ifmatch")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:precondition-required")))
                .andExpect(jsonPath("$.code", is("PRECONDITION_REQUIRED")));
    }

    @Test
    @DisplayName("412 Precondition Failed: Cuando la versión If-Match no coincide")
    void shouldReturn412WhenVersionMismatchOccurs() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(ReviewDecision.APPROVED, "Verificación");

        when(idempotencyRepository.findByKeyAndMethodAndPath(any(), any(), any())).thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class)))
                .thenThrow(new VersionMismatchException(8L, 7L));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-412-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:precondition-failed")))
                .andExpect(jsonPath("$.code", is("PRECONDITION_FAILED")));
    }

    @Test
    @DisplayName("404 Not Found: Cuando el documento no existe o no pertenece al conductor")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(ReviewDecision.APPROVED, "Verificación");

        when(idempotencyRepository.findByKeyAndMethodAndPath(any(), any(), any())).thenReturn(Optional.empty());
        when(reviewDocumentUseCase.reviewDocument(any(ReviewDocumentCommand.class)))
                .thenThrow(new DocumentNotFoundException(documentId));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-404-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:not-found")))
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("400 Bad Request: Cuando la Idempotency-Key tiene menos de 16 caracteres")
    void shouldReturn400WhenIdempotencyKeyIsTooShort() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(ReviewDecision.APPROVED, "Verificación");

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", "too-short")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:bad-request")))
                .andExpect(jsonPath("$.detail", containsString("Idempotency-Key must be between 16 and 128 characters")));
    }

    @Test
    @DisplayName("409 Conflict: Cuando la clave idempotente se reenvía con otro payload")
    void shouldReturn409WhenIdempotencyKeyIsReusedWithDifferentPayload() throws Exception {
        ReviewDocumentRequest request = new ReviewDocumentRequest(ReviewDecision.REJECTED, "Otro motivo distinto");

        IdempotencyRecord previousRecord = new IdempotencyRecord(
                UUID.randomUUID(), idempotencyKey, "OPERATOR", "POST",
                "/api/v1/drivers/" + driverId + "/documents/" + documentId + "/reviews",
                "hash-original-diferente", 200, "{}", Instant.now()
        );

        when(idempotencyRepository.findByKeyAndMethodAndPath(any(), any(), any()))
                .thenReturn(Optional.of(previousRecord));

        mockMvc.perform(post("/api/v1/drivers/{driverId}/documents/{documentId}/reviews", driverId, documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"7\"")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Correlation-ID", "corr-409-test")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("urn:m3:problem:conflict")))
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD")));
    }
}
