package ar.edu.utn.frre.m3.application.trip;

import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;

/**
 * M5 informó que la oferta ya no admite respuesta: no existe para ese conductor (404), la solicitud ya se
 * asignó o quedó sin disponibilidad (409), o la oferta venció o se canceló (410). Reintentar no sirve.
 */
public class OfferUnavailableException extends DomainValidationException {
    public OfferUnavailableException(String message) {
        super(message);
    }
}
