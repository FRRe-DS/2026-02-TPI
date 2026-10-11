package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.IdempotencyRecord;
import java.util.Optional;

public interface IdempotencyRepository {
    Optional<IdempotencyRecord> findByKeyAndMethodAndPath(String key, String method, String path);
    void save(IdempotencyRecord record);
}
