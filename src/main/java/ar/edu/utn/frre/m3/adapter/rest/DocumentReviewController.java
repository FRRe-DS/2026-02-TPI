package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.DocumentResponse;
import ar.edu.utn.frre.m3.adapter.rest.dto.ReviewDocumentRequest;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.usecase.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.usecase.ReviewDocumentUseCase;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.IdempotencyRecord;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import ar.edu.utn.frre.m3.domain.exception.PreconditionRequiredException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/documents/{documentId}/reviews")
@Tag(name = "Documents", description = "Operaciones de documentación y revisión")
public class DocumentReviewController {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final ReviewDocumentUseCase reviewDocumentUseCase;
    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public DocumentReviewController(ReviewDocumentUseCase reviewDocumentUseCase,
                                    IdempotencyRepository idempotencyRepository,
                                    ObjectMapper objectMapper) {
        this.reviewDocumentUseCase = reviewDocumentUseCase;
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reviewDocument(
            @PathVariable UUID driverId,
            @PathVariable UUID documentId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatchHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationIdHeader,
            @Valid @RequestBody ReviewDocumentRequest request,
            HttpServletRequest servletRequest) throws JsonProcessingException {

        String correlationId = (correlationIdHeader != null && !correlationIdHeader.isBlank())
                ? correlationIdHeader
                : UUID.randomUUID().toString();
        servletRequest.setAttribute(CORRELATION_ID_HEADER, correlationId);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() < 16 || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must be between 16 and 128 characters");
        }

        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new PreconditionRequiredException("If-Match header is required for reviewing documents");
        }
        String sanitizedIfMatch = ifMatchHeader.replaceAll("^\"|\"$", "").trim();
        if (!sanitizedIfMatch.matches("^[1-9][0-9]*$")) {
            throw new IllegalArgumentException("If-Match header must match format ^[1-9][0-9]*$");
        }
        long ifMatchVersion = Long.parseLong(sanitizedIfMatch);

        String requestPath = servletRequest.getRequestURI();
        String requestPayload = request.decision().name() + ":" + request.reason();
        String requestHash = computeSha256(requestPayload);

        Optional<IdempotencyRecord> existingRecord = idempotencyRepository
                .findByKeyAndMethodAndPath(idempotencyKey, "POST", requestPath);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency-Key was already used with a different request payload");
            }
            DocumentResponse cachedResponse = objectMapper.readValue(record.getResponseBody(), DocumentResponse.class);
            return ResponseEntity.status(record.getResponseStatus())
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .header(HttpHeaders.ETAG, "\"" + cachedResponse.driverVersion() + "\"")
                    .body(cachedResponse);
        }

        String actor = java.util.Objects.toString(servletRequest.getAttribute("m3.actor"), "LOCAL_OPERATOR");
        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                request.decision(),
                request.reason(),
                ifMatchVersion,
                actor,
                correlationId
        );

        Document updatedDocument = reviewDocumentUseCase.reviewDocument(command);
        DocumentResponse responseBody = DocumentResponse.fromDomain(updatedDocument);

        String serializedBody = objectMapper.writeValueAsString(responseBody);
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                UUID.randomUUID(),
                idempotencyKey,
                actor,
                "POST",
                requestPath,
                requestHash,
                HttpStatus.OK.value(),
                serializedBody,
                Instant.now()
        );
        idempotencyRepository.save(idempotencyRecord);

        return ResponseEntity.ok()
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(HttpHeaders.ETAG, "\"" + updatedDocument.getDriverVersion() + "\"")
                .body(responseBody);
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
