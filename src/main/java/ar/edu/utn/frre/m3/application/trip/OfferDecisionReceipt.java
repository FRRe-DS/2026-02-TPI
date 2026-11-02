package ar.edu.utn.frre.m3.application.trip;

/** M5 acknowledgment for a driver's response to an offer. */
public record OfferDecisionReceipt(String status, String tripId) {
}
