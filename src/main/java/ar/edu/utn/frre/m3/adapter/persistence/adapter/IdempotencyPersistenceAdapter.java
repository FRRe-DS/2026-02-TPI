package ar.edu.utn.frre.m3.adapter.persistence.adapter;

import ar.edu.utn.frre.m3.adapter.persistence.entity.IdempotencyRecordJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataIdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.out.IdempotencyRepositoryPort;
import ar.edu.utn.frre.m3.domain.model.IdempotencyRecord;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdempotencyPersistenceAdapter implements IdempotencyRepositoryPort {

    private final SpringDataIdempotencyRepository idempotencyRepository;

    public IdempotencyPersistenceAdapter(SpringDataIdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    public Optional<IdempotencyRecord> findByKeyAndMethodAndPath(String key, String method, String path) {
        return idempotencyRepository.findByIdempotencyKeyAndHttpMethodAndRequestPath(key, method, path)
                .map(this::toDomain);
    }

    @Override
    public void save(IdempotencyRecord record) {
        IdempotencyRecordJpaEntity entity = toEntity(record);
        idempotencyRepository.save(entity);
    }

    private IdempotencyRecord toDomain(IdempotencyRecordJpaEntity entity) {
        return new IdempotencyRecord(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getPrincipal(),
                entity.getHttpMethod(),
                entity.getRequestPath(),
                entity.getRequestHash(),
                entity.getResponseStatus(),
                entity.getResponseBody(),
                entity.getCreatedAt()
        );
    }

    private IdempotencyRecordJpaEntity toEntity(IdempotencyRecord domain) {
        return new IdempotencyRecordJpaEntity(
                domain.getId(),
                domain.getIdempotencyKey(),
                domain.getPrincipal(),
                domain.getHttpMethod(),
                domain.getRequestPath(),
                domain.getRequestHash(),
                domain.getResponseStatus(),
                domain.getResponseBody(),
                domain.getCreatedAt()
        );
    }
}
