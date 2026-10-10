package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        DocumentType documentType,
        @JsonProperty("kind") DocumentType kind,
        String documentNumber,
        String issuingEntity,
        int revision,
        DocumentStatus status,
        @JsonProperty("reviewStatus") DocumentStatus reviewStatus,
        Instant issuedAt,
        Instant validUntil,
        @JsonProperty("isCurrent") boolean isCurrent,
        String storageReference,
        Long driverVersion,
        Instant createdAt
) {
    public static DocumentResponse fromDomain(Document document) {
        if (document == null) return null;
        return new DocumentResponse(
                document.getId(),
                document.getDriverId(),
                document.getVehicleId(),
                document.getDocumentType(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssuingEntity(),
                document.getRevision(),
                document.getStatus(),
                document.getStatus(),
                document.getIssuedAt(),
                document.getValidUntil(),
                document.isCurrent(),
                document.getStorageReference(),
                document.getDriverVersion(),
                document.getCreatedAt()
        );
    }
}