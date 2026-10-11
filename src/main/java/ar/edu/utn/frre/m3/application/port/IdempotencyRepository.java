package ar.edu.utn.frre.m3.application.port;

import java.util.Optional;

/**
 * Output port for idempotency record management.
 *
 * <p>Records are keyed by {@code (principal, httpMethod, path, idempotencyKey)}.
 * The {@code requestHash} (SHA-256 of canonical body) detects same-key + different-body conflicts.
 */
public interface IdempotencyRepository {

    /**
     * Looks up an existing record by its composite key.
     */
    Optional<IdempotencyRecord> findByKey(
            String principal,
            String httpMethod,
            String path,
            String idempotencyKey);

    /**
     * Saves a new idempotency record inside the current transaction.
     */
    void save(IdempotencyRecord record);

    /**
     * Value object holding the result of a previous identical request.
     */
    record IdempotencyRecord(
            String principal,
            String httpMethod,
            String path,
            String idempotencyKey,
            String requestHash,
            int responseStatus,
            String responseBody
    ) {}
}
