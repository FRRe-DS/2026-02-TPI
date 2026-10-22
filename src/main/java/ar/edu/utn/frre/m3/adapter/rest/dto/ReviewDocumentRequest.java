package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.ReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload to review a driver or vehicle document")
public record ReviewDocumentRequest(
        @NotNull(message = "decision is required")
        @Schema(description = "Review decision", example = "APPROVED", requiredMode = Schema.RequiredMode.REQUIRED)
        ReviewDecision decision,

        @Size(max = 500, message = "reason must not exceed 500 characters")
        @Schema(description = "Optional when approving; required when observing or rejecting", example = "Documentation verified successfully")
        String reason
) {
    @AssertTrue(message = "Debe indicar un motivo para observar o rechazar el documento.")
    @Schema(hidden = true)
    public boolean isReasonProvidedForNonApproval() {
        return decision == null || !decision.requiresReason() || (reason != null && !reason.isBlank());
    }
}
