package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.ServiceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

// The public API does not disclose the license number or storage location.
public record DriverLicenseResponse(UUID id, UUID driverId, UUID vehicleId,
                                    DocumentType kind, int revision,
                                    DocumentStatus reviewStatus, Instant issuedAt,
                                    Instant validUntil, @JsonProperty("isCurrent") boolean isCurrent,
                                    Set<ServiceType> serviceTypes, long driverVersion,
                                    Instant createdAt) {
    public static DriverLicenseResponse from(Document document) {
        return new DriverLicenseResponse(document.getId(), document.getDriverId(), null,
                document.getDocumentType(), document.getRevision(), document.getStatus(),
                document.getIssuedAt(), document.getValidUntil(), document.isCurrent(),
                document.getServiceTypes(), document.getDriverVersion(), document.getCreatedAt());
    }
}
