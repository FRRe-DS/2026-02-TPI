package ar.edu.utn.frre.m3.application.port;

import java.util.UUID;
import ar.edu.utn.frre.m3.application.trip.OfferDecisionReceipt;

/** Outbound M5 port for the driver's response to a dispatch offer. */
public interface M5OfferPort {
    OfferDecisionReceipt respondToOffer(UUID offerId, UUID driverId, String decision);
}
