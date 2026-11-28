package ar.edu.utn.frre.m3.application.trip;

import ar.edu.utn.frre.m3.application.location.GeoPosition;
import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.M5OfferPort;
import ar.edu.utn.frre.m3.application.port.M6TripLifecyclePort;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.IntegrationUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DriverTripWorkflowServiceTest {
    private final UUID driverId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID offerId = UUID.randomUUID();
    private final Instant instant = Instant.parse("2026-11-01T22:30:00Z");
    private DriverRepository drivers;
    private AvailabilityIntentRepository availability;
    private M5OfferPort m5;
    private M6TripLifecyclePort m6;
    private LocationSimulationService locations;
    private DriverTripWorkflowService workflow;
    private DriverOffer offer;

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class);
        availability = mock(AvailabilityIntentRepository.class);
        m5 = mock(M5OfferPort.class);
        m6 = mock(M6TripLifecyclePort.class);
        locations = mock(LocationSimulationService.class);
        workflow = new DriverTripWorkflowService(drivers, availability, m5, m6, locations,
                Clock.fixed(instant, ZoneOffset.UTC));
        when(drivers.findById(driverId)).thenReturn(Optional.of(mock(Driver.class)));
        when(availability.findByDriverId(driverId)).thenReturn(Optional.of(
                AvailabilityIntent.create(driverId, vehicleId, true, true, instant)));
        offer = new DriverOffer(offerId, driverId, "request-12", "Origen", "Destino", "AUTO",
                OffsetDateTime.ofInstant(instant.plusSeconds(120), ZoneOffset.ofHours(-3)),
                DriverOffer.OfferStatus.PENDING, null);
    }

    @Test
    void acceptanceStartsAndCompletesTripAndRepeatedM6CompletionIsIdempotent() {
        workflow.receiveOffer(offer);
        when(m5.respondToOffer(offerId, driverId, "ACCEPTED"))
                .thenReturn(new OfferDecisionReceipt("ACCEPTED", "trip-42"));

        DriverOffer accepted = workflow.respond(driverId, offerId, "ACCEPTED");
        assertEquals(DriverOffer.OfferStatus.ACCEPTED, accepted.status());
        assertEquals(DriverTrip.TripStatus.ASSIGNED, workflow.currentTrip(driverId).status());
        verify(locations).setOccupied(driverId, true);

        workflow.start(driverId, "trip-42");
        verify(m6).startTrip("trip-42", driverId);
        workflow.requestCompletion(driverId, "trip-42");
        verify(m6).requestTripCompletion("trip-42", driverId);

        GeoPosition destination = new GeoPosition(-27.4698, -58.8306);
        DriverTrip first = workflow.completedByM6("trip-42", driverId, destination, 5, null);
        DriverTrip duplicate = workflow.completedByM6("trip-42", driverId, destination, 5, null);

        assertSame(first, duplicate);
        assertEquals(DriverTrip.TripStatus.FINALIZADO, duplicate.status());
        assertEquals(destination, duplicate.destination());
        assertEquals(5, duplicate.driverRating());
        assertEquals(5.0, workflow.ratingSummary(driverId).average());
        assertEquals(1, workflow.ratingSummary(driverId).count());
        verify(locations, times(1)).setCompletedTripDestination(driverId, "FINALIZADO", destination);
    }

    @Test
    void duplicateM5OfferNotificationDoesNotResetItsAcceptedState() {
        workflow.receiveOffer(offer);
        when(m5.respondToOffer(offerId, driverId, "REJECTED"))
                .thenReturn(new OfferDecisionReceipt("REJECTED", null));
        workflow.respond(driverId, offerId, "REJECTED");

        DriverOffer duplicate = workflow.receiveOffer(offer);

        assertEquals(DriverOffer.OfferStatus.REJECTED, duplicate.status());
        assertTrue(workflow.offersFor(driverId).isEmpty());
    }

    @Test
    void cannotAcceptAnOfferForAnotherDriver() {
        workflow.receiveOffer(offer);
        assertThrows(DomainValidationException.class, () -> workflow.respond(UUID.randomUUID(), offerId, "ACCEPTED"));
        verifyNoInteractions(m5);
    }

    @Test
    void cannotAcceptAfterDriverDisconnectsOrBecomesUnavailable() {
        workflow.receiveOffer(offer);
        when(availability.findByDriverId(driverId)).thenReturn(Optional.of(
                AvailabilityIntent.create(driverId, vehicleId, false, false, instant)));

        assertThrows(DomainValidationException.class, () -> workflow.respond(driverId, offerId, "ACCEPTED"));
        verifyNoInteractions(m5);
    }

    // ── T8 · Fin y cancelación según M6, y ofertas cerradas por M5 ──────────────────────────────

    private void anaAcceptsTrip42() {
        workflow.receiveOffer(offer);
        when(m5.respondToOffer(offerId, driverId, "ACCEPTED")).thenReturn(new OfferDecisionReceipt("ACCEPTED", "trip-42"));
        workflow.respond(driverId, offerId, "ACCEPTED");
    }

    private M6TripState m6Says(String state, Double finalLat, Double finalLon) {
        return new M6TripState("trip-42", driverId, "client-7", state, finalLat, finalLon, -27.4698, -58.8306,
                state.equals("COMPLETED") ? OffsetDateTime.parse("2026-11-01T19:50:00-03:00") : null);
    }

    @Test
    void m6CompletedTripFreesAnaAndMovesHerToTheFinalPoint() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("COMPLETED", -27.47, -58.83)));

        assertEquals(1, workflow.syncWithM6());

        DriverTrip trip = workflow.currentTrip(driverId);
        assertEquals(DriverTrip.TripStatus.FINALIZADO, trip.status());
        assertEquals(OffsetDateTime.parse("2026-11-01T19:50:00-03:00"), trip.completedAt());
        verify(locations).setOccupied(driverId, false);
        verify(locations).setCompletedTripDestination(driverId, "FINALIZADO", new GeoPosition(-27.47, -58.83));
        assertEquals(0, workflow.syncWithM6(), "un viaje cerrado no se vuelve a consultar");
        verify(m6, times(1)).findTrip("trip-42");
    }

    @Test
    void withoutFinalPointTheTripDestinationIsUsed() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("COMPLETED", null, null)));
        workflow.syncWithM6();
        verify(locations).setCompletedTripDestination(driverId, "FINALIZADO", new GeoPosition(-27.4698, -58.8306));
    }

    @Test
    void positionThatCannotReachM4DoesNotKeepAnaBusy() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("COMPLETED", -27.47, -58.83)));
        doThrow(new IntegrationUnavailableException("M4 caído")).when(locations)
                .setCompletedTripDestination(eq(driverId), eq("FINALIZADO"), any());

        assertEquals(1, workflow.syncWithM6());
        assertEquals(DriverTrip.TripStatus.FINALIZADO, workflow.currentTrip(driverId).status());
        verify(locations).setOccupied(driverId, false);
    }

    @Test
    void m6CancelledTripFreesAnaAndSheCanReceiveANewOffer() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("CANCELLED", null, null)));

        assertEquals(1, workflow.syncWithM6());

        assertEquals(DriverTrip.TripStatus.CANCELADO, workflow.currentTrip(driverId).status());
        verify(locations).setOccupied(driverId, false);
        verify(locations, never()).setCompletedTripDestination(any(), any(), any());
        var next = new DriverOffer(UUID.randomUUID(), driverId, "request-13", "Origen", "Destino", "AUTO",
                offer.expiresAt(), DriverOffer.OfferStatus.PENDING, null);
        assertEquals(DriverOffer.OfferStatus.PENDING, workflow.receiveOffer(next).status());
    }

    @Test
    void m6InProgressMarksTheTripStartedAndKeepsAnaBusy() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("IN_PROGRESS", null, null)));

        assertEquals(1, workflow.syncWithM6());
        assertEquals(DriverTrip.TripStatus.IN_PROGRESS, workflow.currentTrip(driverId).status());
        verify(locations, never()).setOccupied(driverId, false);
        assertEquals(0, workflow.syncWithM6(), "sigue en curso: no hay cambio nuevo");
    }

    @Test
    void m6WaitingForPickupDoesNotChangeAnything() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(m6Says("DRIVER_ARRIVED", null, null)));
        assertEquals(0, workflow.syncWithM6());
        assertEquals(DriverTrip.TripStatus.ASSIGNED, workflow.currentTrip(driverId).status());
    }

    @Test
    void unavailableOrUnknownOrForeignTripsInM6KeepTheLocalState() {
        anaAcceptsTrip42();
        when(m6.findTrip("trip-42")).thenThrow(new IntegrationUnavailableException("M6 caído"));
        assertEquals(0, workflow.syncWithM6());

        reset(m6);
        when(m6.findTrip("trip-42")).thenReturn(Optional.empty());
        assertEquals(0, workflow.syncWithM6());

        reset(m6);
        when(m6.findTrip("trip-42")).thenReturn(Optional.of(new M6TripState("trip-42", UUID.randomUUID(), "c",
                "COMPLETED", null, null, null, null, null)));
        assertEquals(0, workflow.syncWithM6());

        assertEquals(DriverTrip.TripStatus.ASSIGNED, workflow.currentTrip(driverId).status());
        verify(locations, never()).setOccupied(driverId, false);
    }

    @Test
    void offerClosedByM5StopsBeingPendingAndCreatesNoTrip() {
        workflow.receiveOffer(offer);
        when(m5.respondToOffer(offerId, driverId, "ACCEPTED"))
                .thenThrow(new OfferUnavailableException("La solicitud ya fue asignada o quedó sin disponibilidad en M5."));

        assertThrows(OfferUnavailableException.class, () -> workflow.respond(driverId, offerId, "ACCEPTED"));

        assertTrue(workflow.offersFor(driverId).isEmpty());
        assertNull(workflow.currentTrip(driverId));
        verify(locations, never()).setOccupied(driverId, true);
    }

    @Test
    void temporaryM5FailureKeepsTheOfferPendingToRetry() {
        workflow.receiveOffer(offer);
        when(m5.respondToOffer(offerId, driverId, "ACCEPTED")).thenThrow(new IntegrationUnavailableException("503"));

        assertThrows(IntegrationUnavailableException.class, () -> workflow.respond(driverId, offerId, "ACCEPTED"));

        assertEquals(1, workflow.offersFor(driverId).size());
        assertNull(workflow.currentTrip(driverId));
    }
}
