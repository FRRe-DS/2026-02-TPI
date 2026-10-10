package ar.edu.utn.frre.m3.adapter.persistence.adapter;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AuditLogJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataAuditLogRepository;
import ar.edu.utn.frre.m3.application.port.out.AuditLogRepositoryPort;
import ar.edu.utn.frre.m3.domain.model.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final SpringDataAuditLogRepository auditLogRepository;

    public AuditLogPersistenceAdapter(SpringDataAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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
        auditLogRepository.save(entity);
    }
}
