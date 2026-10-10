package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.DocumentResponse;
import ar.edu.utn.frre.m3.adapter.rest.dto.ReviewDocumentRequest;
import ar.edu.utn.frre.m3.adapter.rest.mapper.DocumentRestMapper;
import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentUseCase;
import ar.edu.utn.frre.m3.application.port.out.IdempotencyRepositoryPort;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import ar.edu.utn.frre.m3.domain.exception.PreconditionRequiredException;
import ar.edu.utn.frre.m3.domain.model.Document;
import ar.edu.utn.frre.m3.domain.model.IdempotencyRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/documents/{documentId}/reviews")
@Tag(name = "Documents", description = "Document management and review operations")
public class DocumentReviewController {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final ReviewDocumentUseCase reviewDocumentUseCase;
    private final DocumentRestMapper documentRestMapper;
    private final IdempotencyRepositoryPort idempotencyRepositoryPort;
    private final ObjectMapper objectMapper;

    public DocumentReviewController(ReviewDocumentUseCase reviewDocumentUseCase,
                                    DocumentRestMapper documentRestMapper,
                                    IdempotencyRepositoryPort idempotencyRepositoryPort,
                                    ObjectMapper objectMapper) {
        this.reviewDocumentUseCase = reviewDocumentUseCase;
        this.documentRestMapper = documentRestMapper;
        this.idempotencyRepositoryPort = idempotencyRepositoryPort;
        this.objectMapper = objectMapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "reviewDocument",
            summary = "Revisar documento con motivo",
            description = "Admite documento del conductor o de uno de sus vehículos; comprobar la relación completa. "
                    + "Crea auditoría, no cambia contenido de la revisión."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Respuesta correcta.",
                    headers = {
                            @Header(name = "X-Correlation-ID", description = "Correlation identifier", schema = @Schema(type = "string")),
                            @Header(name = "ETag", description = "Driver aggregate version ETag", schema = @Schema(type = "string", example = "\"7\""))
                    },
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DocumentResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o campos no permitidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Recurso inexistente o no visible para este principal.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto o clave idempotente reutilizada con otro payload.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "412", description = "If-Match no coincide con la versión actual.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "428", description = "Falta If-Match requerido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<?> reviewDocument(
            @Parameter(name = "driverId", description = "Driver UUID", in = ParameterIn.PATH, required = true)
            @PathVariable UUID driverId,

            @Parameter(name = "documentId", description = "Document UUID", in = ParameterIn.PATH, required = true)
            @PathVariable UUID documentId,

            @Parameter(name = "If-Match", description = "ETag del agregado conductor (formato regex ^[1-9][0-9]*$)", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatchHeader,

            @Parameter(name = "Idempotency-Key", description = "Clave idempotente (16 a 128 caracteres)", in = ParameterIn.HEADER, required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,

            @Parameter(name = "X-Correlation-ID", description = "Correlation ID opcional", in = ParameterIn.HEADER)
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationIdHeader,

            @Valid @RequestBody ReviewDocumentRequest request,
            HttpServletRequest servletRequest) throws JsonProcessingException {

        // 1. Resolve and set correlation ID
        String correlationId = (correlationIdHeader != null && !correlationIdHeader.isBlank())
                ? correlationIdHeader
                : UUID.randomUUID().toString();
        servletRequest.setAttribute(CORRELATION_ID_HEADER, correlationId);

        // 2. Validate Idempotency-Key header (16 to 128 chars)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() < 16 || idempotencyKey.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key must be between 16 and 128 characters");
        }

        // 3. Validate If-Match header
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new PreconditionRequiredException("If-Match header is required for reviewing documents");
        }
        String sanitizedIfMatch = ifMatchHeader.replaceAll("^\"|\"$", "").trim();
        if (!sanitizedIfMatch.matches("^[1-9][0-9]*$")) {
            throw new IllegalArgumentException("If-Match header must match format ^[1-9][0-9]*$");
        }
        long ifMatchVersion = Long.parseLong(sanitizedIfMatch);

        // 4. Idempotency handling
        String requestPath = servletRequest.getRequestURI();
        String requestPayload = request.decision().name() + ":" + request.reason();
        String requestHash = computeSha256(requestPayload);

        Optional<IdempotencyRecord> existingRecord = idempotencyRepositoryPort
                .findByKeyAndMethodAndPath(idempotencyKey, "POST", requestPath);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency-Key was already used with a different request payload");
            }
            // Idempotency replay: deserialize previous response and return
            DocumentResponse cachedResponse = objectMapper.readValue(record.getResponseBody(), DocumentResponse.class);
            return ResponseEntity.status(record.getResponseStatus())
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .header(HttpHeaders.ETAG, "\"" + cachedResponse.driverVersion() + "\"")
                    .body(cachedResponse);
        }

        // 5. Execute use case
        String actor = "OPERATOR";
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
        DocumentResponse responseBody = documentRestMapper.toResponse(updatedDocument);

        // 6. Record idempotency
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
        idempotencyRepositoryPort.save(idempotencyRecord);

        // 7. Return 200 OK with ETag and correlation ID
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
