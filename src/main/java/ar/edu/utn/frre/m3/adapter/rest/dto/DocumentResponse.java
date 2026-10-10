package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.model.DocumentKind;
import ar.edu.utn.frre.m3.domain.model.ReviewStatus;
import ar.edu.utn.frre.m3.domain.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Contractual Document response")
public record DocumentResponse(
        @Schema(description = "Document ID", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,

        @Schema(description = "Driver ID", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID driverId,

        @Schema(description = "Vehicle ID (null for driver personal documents)", nullable = true)
        UUID vehicleId,

        @Schema(description = "Document kind", example = "LICENSE", requiredMode = Schema.RequiredMode.REQUIRED)
        DocumentKind kind,

        @Schema(description = "Document revision number", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int revision,

        @Schema(description = "Review status", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
        ReviewStatus reviewStatus,

        @Schema(description = "Date and time of issuance", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant issuedAt,

        @Schema(description = "Expiration date and time", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant validUntil,

        @Schema(description = "Whether this is the current active version", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean isCurrent,

        @Schema(description = "Associated service types", example = "[\"AUTO\"]")
        List<ServiceType> serviceTypes,

        @Schema(description = "Driver aggregate version after mutation", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
        long driverVersion,

        @Schema(description = "Creation date and time", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant createdAt
) {}
