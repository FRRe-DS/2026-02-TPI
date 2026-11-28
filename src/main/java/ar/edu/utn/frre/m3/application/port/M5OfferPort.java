package ar.edu.utn.frre.m3.application.port;

import java.util.UUID;
import ar.edu.utn.frre.m3.application.trip.OfferDecisionReceipt;

/**
 * Puerto de salida hacia M5 para la respuesta del conductor a una oferta.
 * Lanza {@code OfferUnavailableException} si M5 dice que la oferta ya no admite respuesta (404, 409, 410)
 * e {@code IntegrationUnavailableException} si M5 no responde o falla temporalmente (503).
 */
public interface M5OfferPort {
    OfferDecisionReceipt respondToOffer(UUID offerId, UUID driverId, String decision);
}
