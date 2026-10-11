package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AuditLogJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link AuditLogJpaEntity}. */
public interface SpringDataAuditLogRepository
    extends JpaRepository<AuditLogJpaEntity, UUID> {}
