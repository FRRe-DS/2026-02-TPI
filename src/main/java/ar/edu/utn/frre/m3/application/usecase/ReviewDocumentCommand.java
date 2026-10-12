package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.ReviewDecision;
import java.util.UUID;

public record ReviewDocumentCommand(
        UUID driverId,
        UUID documentId,
        ReviewDecision decision,
        String reason,
        long ifMatchVersion,
        String actor,
        String correlationId
) {}
