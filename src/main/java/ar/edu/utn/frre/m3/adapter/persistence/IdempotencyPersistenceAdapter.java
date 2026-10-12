package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.IdempotencyRecordJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataIdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class IdempotencyPersistenceAdapter implements IdempotencyRepository {

    private final SpringDataIdempotencyRepository repository;

    public IdempotencyPersistenceAdapter(SpringDataIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ar.edu.utn.frre.m3.domain.IdempotencyRecord> findByKeyAndMethodAndPath(String key, String method, String path) {
        return repository.findByIdempotencyKeyAndHttpMethodAndRequestPath(key, method, path)
                .map(entity -> new ar.edu.utn.frre.m3.domain.IdempotencyRecord(
                        entity.getId(),
                        entity.getIdempotencyKey(),
                        entity.getPrincipal(),
                        entity.getHttpMethod(),
                        entity.getRequestPath(),
                        entity.getRequestHash(),
                        entity.getResponseStatus(),
                        entity.getResponseBody(),
                        entity.getCreatedAt()
                ));
    }

    @Override
    public void save(ar.edu.utn.frre.m3.domain.IdempotencyRecord record) {
        IdempotencyRecordJpaEntity entity = new IdempotencyRecordJpaEntity(
                record.getId(),
                record.getIdempotencyKey(),
                record.getPrincipal(),
                record.getHttpMethod(),
                record.getRequestPath(),
                record.getRequestHash(),
                record.getResponseStatus(),
                record.getResponseBody(),
                record.getCreatedAt()
        );
        repository.save(entity);
    }
}
