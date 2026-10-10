package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.DocumentType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Command representing the input data needed to register a new vehicle document revision.
 */
public record RegisterVehicleDocumentCommand(
    UUID driverId,
    UUID vehicleId,
    DocumentType documentType,
    String documentNumber,
    String issuingEntity,
    Instant issuedAt,
    Instant validUntil,
    String storageReference
) {
    public RegisterVehicleDocumentCommand {
        Objects.requireNonNull(driverId, "driverId must not be null");
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(documentType, "documentType must not be null");
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("documentNumber must not be null or blank");
        }
        Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        Objects.requireNonNull(validUntil, "validUntil must not be null");
    }
}

