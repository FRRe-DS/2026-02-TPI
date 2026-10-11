package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.OutboxJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link OutboxJpaEntity}. */
public interface SpringDataOutboxRepository
    extends JpaRepository<OutboxJpaEntity, UUID> {

    /** Used by the worker to pick up pending messages for M4. */
    List<OutboxJpaEntity> findTop20ByStatusInOrderByNextAttemptAtAsc(List<String> statuses);
}
