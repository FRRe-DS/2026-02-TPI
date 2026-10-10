package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.model.DocumentKind;
import ar.edu.utn.frre.m3.domain.model.ReviewStatus;
import ar.edu.utn.frre.m3.domain.model.ServiceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contractual Document response representing registration and review states.
 */
@Schema(description = "Contractual Document response")
public record DocumentResponse(
    @Schema(description = "Document ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,

    @Schema(description = "Driver ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID driverId,

    @Schema(description = "Vehicle ID (null for driver personal documents)", nullable = true)
    UUID vehicleId,

    @Schema(description = "Vehicle document type", nullable = true)
    DocumentType documentType,

    @Schema(description = "Document kind", example = "LICENSE", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("kind")
    DocumentKind kind,

    @Schema(description = "Document number", nullable = true)
    String documentNumber,

    @Schema(description = "Issuing entity", nullable = true)
    String issuingEntity,

    @Schema(description = "Document revision number", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    int revision,

    @Schema(description = "Document status", nullable = true)
    DocumentStatus status,

    @Schema(description = "Review status", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("reviewStatus")
    ReviewStatus reviewStatus,

    @Schema(description = "Date and time of issuance", requiredMode = Schema.RequiredMode.REQUIRED)
    Instant issuedAt,

    @Schema(description = "Expiration date and time", requiredMode = Schema.RequiredMode.REQUIRED)
    Instant validUntil,

    @Schema(description = "Whether this is the current active version", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isCurrent")
    boolean isCurrent,

    @Schema(description = "Storage reference", nullable = true)
    String storageReference,

    @Schema(description = "Associated service types", example = "[\"AUTO\"]", nullable = true)
    List<ServiceType> serviceTypes,

    @Schema(description = "Driver aggregate version after mutation", example = "8", nullable = true)
    Long driverVersion,

    @Schema(description = "Creation date and time", requiredMode = Schema.RequiredMode.REQUIRED)
    Instant createdAt
) {

    public DocumentResponse(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        DocumentKind kind,
        int revision,
        ReviewStatus reviewStatus,
        Instant issuedAt,
        Instant validUntil,
        boolean isCurrent,
        List<ServiceType> serviceTypes,
        Long driverVersion,
        Instant createdAt
    ) {
        this(
            id,
            driverId,
            vehicleId,
            mapKindToDocumentType(kind),
            kind,
            null,
            null,
            revision,
            mapReviewStatusToDocumentStatus(reviewStatus),
            reviewStatus,
            issuedAt,
            validUntil,
            isCurrent,
            null,
            serviceTypes,
            driverVersion,
            createdAt
        );
    }

    public DocumentResponse(
        UUID id,
        UUID driverId,
        UUID vehicleId,
        DocumentType documentType,
        DocumentType kindType,
        String documentNumber,
        String issuingEntity,
        int revision,
        DocumentStatus status,
        DocumentStatus reviewStatusEnum,
        Instant issuedAt,
        Instant validUntil,
        boolean isCurrent,
        String storageReference,
        Instant createdAt
    ) {
        this(
            id,
            driverId,
            vehicleId,
            documentType,
            mapDocumentTypeToKind(kindType != null ? kindType : documentType),
            documentNumber,
            issuingEntity,
            revision,
            status,
            mapDocumentStatusToReviewStatus(reviewStatusEnum != null ? reviewStatusEnum : status),
            issuedAt,
            validUntil,
            isCurrent,
            storageReference,
            null,
            null,
            createdAt
        );
    }

    public static DocumentResponse fromDomain(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
            document.getId(),
            document.getDriverId(),
            document.getVehicleId(),
            document.getDocumentType(),
            mapDocumentTypeToKind(document.getDocumentType()),
            document.getDocumentNumber(),
            document.getIssuingEntity(),
            document.getRevision(),
            document.getStatus(),
            mapDocumentStatusToReviewStatus(document.getStatus()),
            document.getIssuedAt(),
            document.getValidUntil(),
            document.isCurrent(),
            document.getStorageReference(),
            null,
            null,
            document.getCreatedAt()
        );
    }

    private static DocumentType mapKindToDocumentType(DocumentKind kind) {
        if (kind == null) return null;
        try {
            return DocumentType.valueOf(kind.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static DocumentKind mapDocumentTypeToKind(DocumentType type) {
        if (type == null) return null;
        try {
            return DocumentKind.valueOf(type.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static DocumentStatus mapReviewStatusToDocumentStatus(ReviewStatus status) {
        if (status == null) return null;
        try {
            return DocumentStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ReviewStatus mapDocumentStatusToReviewStatus(DocumentStatus status) {
        if (status == null) return null;
        try {
            return ReviewStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
