package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.exception.DriverAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manejador global de excepciones para la API REST según la sección 9.4
 * (RFC 9457 ProblemDetail).
 */
@RestControllerAdvice(basePackages = "ar.edu.utn.frre.m3.adapter.rest")
public class GlobalExceptionHandler {

    @ExceptionHandler(DriverAlreadyExistsException.class)
    public ProblemDetail handleDriverAlreadyExists(DriverAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setType(URI.create("https://utn.edu.ar/errors/driver-already-exists"));
        problem.setTitle("Conflicto de identidad de conductor");
        problem.setProperty("code", "DRIVER_ALREADY_EXISTS");
        problem.setProperty("correlationId", UUID.randomUUID().toString());
        problem.setProperty("identitySubject", ex.getIdentitySubject());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos inválidos o incompletos"
        );
        problem.setType(URI.create("https://utn.edu.ar/errors/validation-error"));
        problem.setTitle("Error de validación en la solicitud");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("correlationId", UUID.randomUUID().toString());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("invalidFields", fieldErrors);

        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setType(URI.create("https://utn.edu.ar/errors/bad-request"));
        problem.setTitle("Parámetro inválido");
        problem.setProperty("code", "INVALID_ARGUMENT");
        problem.setProperty("correlationId", UUID.randomUUID().toString());
        return problem;
    }
}
