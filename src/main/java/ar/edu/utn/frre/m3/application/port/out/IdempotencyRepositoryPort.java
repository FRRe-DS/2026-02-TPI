package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.model.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyRepositoryPort {
    Optional<IdempotencyRecord> findByKeyAndMethodAndPath(String key, String method, String path);
    void save(IdempotencyRecord record);
}
