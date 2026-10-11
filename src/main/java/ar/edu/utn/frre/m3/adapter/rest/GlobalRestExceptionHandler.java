package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotQualifiedException;
import ar.edu.utn.frre.m3.domain.exception.IdempotencyConflictException;
import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global REST exception handler producing RFC 9457 ProblemDetail (application/problem+json).
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";

    // ── HU-10: Availability Intent exceptions ─────────────────────────────────

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDriverNotFound(
        DriverNotFoundException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:driver-not-found"));
        problem.setTitle("Driver Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "DRIVER_NOT_FOUND");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(DriverNotQualifiedException.class)
    public ResponseEntity<ProblemDetail> handleDriverNotQualified(
        DriverNotQualifiedException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:driver-not-qualified"));
        problem.setTitle("Driver Not Qualified");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "DRIVER_NOT_QUALIFIED");
        problem.setProperty("reason", ex.getReason());
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(
        IdempotencyConflictException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:idempotency-conflict"));
        problem.setTitle("Idempotency Key Conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "IDEMPOTENCY_CONFLICT");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    // ── Existing handlers ─────────────────────────────────────────────────────

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleVehicleNotFound(
        VehicleNotFoundException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:not-found"));
        problem.setTitle("Vehicle Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VEHICLE_NOT_FOUND");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(VehicleForbiddenException.class)
    public ResponseEntity<ProblemDetail> handleVehicleForbidden(
        VehicleForbiddenException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:forbidden"));
        problem.setTitle("Vehicle Access Forbidden");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VEHICLE_FORBIDDEN");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(InvalidDocumentPeriodException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPeriod(
        InvalidDocumentPeriodException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:invalid-period"));
        problem.setTitle("Invalid Document Period");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INVALID_DOCUMENT_PERIOD");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> Map.of(
                "field", err.getField(),
                "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"
            ))
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        );
        problem.setType(URI.create("urn:m3:problem:validation"));
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("correlationId", resolveCorrelationId(request));
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMalformedJson(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request body: " + ex.getMostSpecificCause().getMessage()
        );
        problem.setType(URI.create("urn:m3:problem:malformed-request"));
        problem.setTitle("Malformed JSON Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "MALFORMED_REQUEST");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:m3:problem:bad-request"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "BAD_REQUEST");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred: " + ex.getMessage()
        );
        problem.setType(URI.create("urn:m3:problem:internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "INTERNAL_SERVER_ERROR");
        problem.setProperty("correlationId", resolveCorrelationId(request));

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header(CORRELATION_HEADER, (String) problem.getProperties().get("correlationId"))
            .body(problem);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}

