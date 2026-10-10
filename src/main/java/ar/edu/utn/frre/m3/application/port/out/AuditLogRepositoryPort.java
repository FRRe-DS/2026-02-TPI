package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.model.AuditLog;

public interface AuditLogRepositoryPort {
    void save(AuditLog auditLog);
}
