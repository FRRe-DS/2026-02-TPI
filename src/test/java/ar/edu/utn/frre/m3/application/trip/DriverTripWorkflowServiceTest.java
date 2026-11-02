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
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
}
