package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.domain.model.ReviewDecision;

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
