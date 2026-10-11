package ar.edu.utn.frre.m3.application.port;

import java.util.UUID;

/**
 * Output port for append-only audit logging.
 */
public interface AuditLogRepository {

    /**
     * Appends an audit entry inside the current transaction.
     *
     * @param entityType    e.g. "AvailabilityIntent"
     * @param entityId      UUID of the affected aggregate
     * @param operation     e.g. "CREATE_INTENT" or "UPDATE_INTENT"
     * @param actor         Principal identifier (driverId, system, etc.)
     * @param correlationId Request/trace correlation identifier
     * @param changes       Non-sensitive description of changes (JSON or plain text)
     */
    void append(
            String entityType,
            UUID entityId,
            String operation,
            String actor,
            String correlationId,
            String changes);
}
