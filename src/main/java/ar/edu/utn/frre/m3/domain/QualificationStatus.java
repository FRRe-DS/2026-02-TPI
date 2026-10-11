package ar.edu.utn.frre.m3.domain;

/**
 * Representa los estados de habilitación/calificación del conductor
 * según la sección 6.5 del documento de arquitectura.
 */
public enum QualificationStatus {
    PENDING,
    APPROVED,
    OBSERVED,
    REJECTED,
    SUSPENDED
}
