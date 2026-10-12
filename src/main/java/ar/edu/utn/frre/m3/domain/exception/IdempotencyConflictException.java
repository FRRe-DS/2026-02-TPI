package ar.edu.utn.frre.m3.domain.exception;

/**
 * Thrown when an {@code Idempotency-Key} is reused with a different request body.
 * Maps to HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String messageOrKey) {
        super(messageOrKey != null && messageOrKey.contains(" ")
                ? messageOrKey
                : "Idempotency-Key '" + messageOrKey + "' was already used with a different request body.");
        this.idempotencyKey = messageOrKey;
    }

    public IdempotencyConflictException(String message, String idempotencyKey) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
