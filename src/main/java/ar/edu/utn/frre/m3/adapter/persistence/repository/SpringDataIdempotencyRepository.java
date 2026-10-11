package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.IdempotencyRecordJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link IdempotencyRecordJpaEntity}. */
public interface SpringDataIdempotencyRepository
    extends JpaRepository<IdempotencyRecordJpaEntity, UUID> {

    Optional<IdempotencyRecordJpaEntity> findByPrincipalAndHttpMethodAndPathAndIdempotencyKey(
            String principal,
            String httpMethod,
            String path,
            String idempotencyKey);
}
