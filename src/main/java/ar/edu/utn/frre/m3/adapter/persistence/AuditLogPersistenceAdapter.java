package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AuditLogJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataAuditLogRepository;
import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.domain.AuditLog;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogPersistenceAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository repository;

    public AuditLogPersistenceAdapter(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AuditLog auditLog) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity(
                auditLog.getId(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDriverId(),
                auditLog.getAction(),
                auditLog.getActor(),
                auditLog.getPreviousStatus(),
                auditLog.getNewStatus(),
                auditLog.getReason(),
                auditLog.getCorrelationId(),
                auditLog.getCreatedAt()
        );
        repository.save(entity);
    }
}
