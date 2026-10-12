package ar.edu.utn.frre.m3.domain.eligibility;

import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.QualificationStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentSnapshot(
        UUID id,
        DocumentType type,
        QualificationStatus qualificationStatus,
        Instant validUntil, Instant issuedAt, java.util.Set<ar.edu.utn.frre.m3.domain.ServiceType> serviceTypes
) {
    public boolean isExpiredAt(Instant now) {
        return validUntil != null && !now.isBefore(validUntil);
    }
}
