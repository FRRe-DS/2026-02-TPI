package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.ReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload to review a driver or vehicle document")
public record ReviewDocumentRequest(
        @NotNull(message = "decision is required")
        @Schema(description = "Review decision", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
        ReviewDecision decision,

        @NotNull(message = "reason is required")
        @Size(min = 1, max = 500, message = "reason must be between 1 and 500 characters")
        @Schema(description = "Justification or reason for the decision", example = "Documentation verified successfully", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {}