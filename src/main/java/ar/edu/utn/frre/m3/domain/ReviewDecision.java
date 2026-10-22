package ar.edu.utn.frre.m3.domain;

public enum ReviewDecision {
    APPROVED,
    OBSERVED,
    REJECTED;

    public boolean requiresReason() {
        return this != APPROVED;
    }

    public String missingReasonMessage() {
        return switch (this) {
            case OBSERVED -> "Debe indicar el motivo de la observación.";
            case REJECTED -> "Debe indicar el motivo del rechazo.";
            case APPROVED -> "";
        };
    }
}
