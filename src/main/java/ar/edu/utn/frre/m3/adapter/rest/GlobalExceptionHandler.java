package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import ar.edu.utn.frre.m3.domain.exception.PreconditionRequiredException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(PreconditionRequiredException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionRequired(PreconditionRequiredException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:precondition-required"));
        problem.setTitle("Precondition Required");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "PRECONDITION_REQUIRED");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.PRECONDITION_REQUIRED, problem, correlationId);
    }

    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<ProblemDetail> handleVersionMismatch(VersionMismatchException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:precondition-failed"));
        problem.setTitle("Precondition Failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "PRECONDITION_FAILED");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.PRECONDITION_FAILED, problem, correlationId);
    }

    @ExceptionHandler({DriverNotFoundException.class, DocumentNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:not-found"));
        problem.setTitle("Resource Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.NOT_FOUND, problem, correlationId);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:conflict"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.CONFLICT, problem, correlationId);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:business-rule-violation"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "UNPROCESSABLE_ENTITY");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem, correlationId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setType(URI.create("urn:m3:problem:validation"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("correlationId", correlationId);

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid value"
                ))
                .toList();
        problem.setProperty("errors", errors);

        return buildResponse(HttpStatus.BAD_REQUEST, problem, correlationId);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        if ("If-Match".equalsIgnoreCase(ex.getHeaderName())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_REQUIRED,
                    "If-Match header is required for state mutating operations");
            problem.setType(URI.create("urn:m3:problem:precondition-required"));
            problem.setTitle("Precondition Required");
            problem.setInstance(URI.create(request.getRequestURI()));
            problem.setProperty("code", "PRECONDITION_REQUIRED");
            problem.setProperty("correlationId", correlationId);
            return buildResponse(HttpStatus.PRECONDITION_REQUIRED, problem, correlationId);
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing required header: " + ex.getHeaderName());
        problem.setType(URI.create("urn:m3:problem:bad-request"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "MISSING_HEADER");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.BAD_REQUEST, problem, correlationId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:bad-request"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "BAD_REQUEST");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.BAD_REQUEST, problem, correlationId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex, HttpServletRequest request) {
        String correlationId = resolveCorrelationId(request);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred");
        problem.setType(URI.create("urn:m3:problem:internal-server-error"));
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INTERNAL_SERVER_ERROR");
        problem.setProperty("correlationId", correlationId);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem, correlationId);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CORRELATION_ID_HEADER);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        String header = request.getHeader(CORRELATION_ID_HEADER);
        if (header != null && !header.isBlank()) {
            return header;
        }
        String generated = UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ID_HEADER, generated);
        return generated;
    }

    private ResponseEntity<ProblemDetail> buildResponse(HttpStatus status, ProblemDetail problem, String correlationId) {
        return ResponseEntity.status(status)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body(problem);
    }
}
