package ar.edu.utn.frre.m3.domain;
import java.time.Instant;
import java.util.UUID;
public record QualificationDecision(UUID id, UUID driverId, UUID vehicleId, QualificationStatus previousStatus,
        QualificationStatus newStatus, String reason, String actorSubject, long driverVersion, Instant occurredAt) {}
