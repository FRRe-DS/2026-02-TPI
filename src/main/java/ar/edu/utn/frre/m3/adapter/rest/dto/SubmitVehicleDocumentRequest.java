package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.DocumentType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request DTO for registering a vehicle document revision.
 * Processes exclusively metadata (no binary file content).
 */
public record SubmitVehicleDocumentRequest(
    @NotNull(message = "documentType is required")
    @JsonAlias({"kind", "documentType"})
    DocumentType documentType,

    @NotBlank(message = "documentNumber is required")
    @Size(max = 80, message = "documentNumber cannot exceed 80 characters")
    String documentNumber,

    @Size(max = 100, message = "issuingEntity cannot exceed 100 characters")
    String issuingEntity,

    @NotNull(message = "issuedAt is required")
    Instant issuedAt,

    @NotNull(message = "validUntil is required")
    Instant validUntil,

    @Size(max = 200, message = "storageReference cannot exceed 200 characters")
    String storageReference
) {}

