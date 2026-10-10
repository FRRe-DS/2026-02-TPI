package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.AuditLog;

public interface AuditLogRepository {
    void save(AuditLog auditLog);
}
