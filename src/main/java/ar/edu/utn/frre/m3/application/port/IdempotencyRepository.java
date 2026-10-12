package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.IdempotencyRecord;
import java.util.Optional;

/**
 * Output port for idempotency record management.
 */
public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByKeyAndMethodAndPath(String key, String method, String path);

    void save(IdempotencyRecord record);

    /**
     * Looks up an existing record by its composite key (HU-10 compatibility).
     */
    default Optional<IdempotencyRecord> findByKey(
            String principal,
            String httpMethod,
            String path,
            String idempotencyKey) {
        return findByKeyAndMethodAndPath(idempotencyKey, httpMethod, path);
    }
}
