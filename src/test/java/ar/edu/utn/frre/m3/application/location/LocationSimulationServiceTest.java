package ar.edu.utn.frre.m3.application.location;

import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.M4LocationPort;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.Vehicle;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationSimulationServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-01T22:30:00Z");
    private final UUID driverId = UUID.fromString("f1f8e742-443d-4fd2-97f3-fadf74db7c8c");
    private final UUID vehicleId = UUID.fromString("7c29dfcf-36d8-46f5-9a88-e41d9ca1092b");
    private AvailabilityIntentRepository intents;
    private DriverRepository drivers;
    private VehicleRepository vehicles;
    private M4LocationPort m4;
    private LocationSimulationService simulation;
    private AvailabilityIntent connectedIntent;

    @BeforeEach
    void setUp() {
        intents = mock(AvailabilityIntentRepository.class);
        drivers = mock(DriverRepository.class);
        vehicles = mock(VehicleRepository.class);
        m4 = mock(M4LocationPort.class);
        simulation = new LocationSimulationService(intents, drivers, vehicles, m4,
                Clock.fixed(NOW, ZoneOffset.UTC), 35);
        connectedIntent = AvailabilityIntent.create(driverId, vehicleId, true, true, NOW);
        Driver driver = approvedDriver();
        Vehicle vehicle = approvedVehicle();
        when(drivers.findById(driverId)).thenReturn(Optional.of(driver));
        when(vehicles.findById(vehicleId)).thenReturn(Optional.of(vehicle));
    }

    @Test
    void connectedDriverMovesInSmallStepsAndEachPointIsSentToM4() {
        when(intents.findConnected()).thenReturn(List.of(connectedIntent));

        simulation.publishConnectedLocations();
        GeoPosition first = simulation.currentPosition(driverId);
        simulation.publishConnectedLocations();
        GeoPosition second = simulation.currentPosition(driverId);

        assertWithinResistencia(first);
        assertWithinResistencia(second);
        assertTrue(distanceMeters(first, second) <= 50, "each update should be a short walk, not a teleport");
        assertTrue(distanceMeters(first, second) > 0, "a connected driver should move");
        var sent = org.mockito.ArgumentCaptor.forClass(LocationUpdate.class);
        verify(m4, times(2)).sendLocation(sent.capture());
        assertEquals(driverId, sent.getAllValues().get(0).driverId());
        assertTrue(sent.getAllValues().get(0).connected());
        assertTrue(sent.getAllValues().get(0).available());
        assertEquals("2026-11-01T19:30-03:00", sent.getAllValues().get(0).recordedAt().toString());
    }

    @Test
    void disconnectedDriverDoesNotMoveOrSendLocation() {
        AvailabilityIntent disconnected = AvailabilityIntent.create(driverId, vehicleId, false, false, NOW);
        when(intents.findConnected()).thenReturn(List.of(disconnected));

        simulation.publishConnectedLocations();

        assertNull(simulation.currentPosition(driverId));
        verifyNoInteractions(m4);
    }

    @Test
    void completedTripRepositionsImmediatelyAndNextStepStartsAtDestination() {
        GeoPosition destination = new GeoPosition(-27.4698, -58.8306);
        when(intents.findByDriverId(driverId)).thenReturn(Optional.of(connectedIntent));
        when(intents.findConnected()).thenReturn(List.of(connectedIntent));

        simulation.setCompletedTripDestination(driverId, "FINALIZADO", destination);
        assertEquals(destination, simulation.currentPosition(driverId));
        var destinationUpdate = org.mockito.ArgumentCaptor.forClass(LocationUpdate.class);
        verify(m4).sendLocation(destinationUpdate.capture());
        assertEquals(destination.latitude(), destinationUpdate.getValue().latitude());
        assertEquals(destination.longitude(), destinationUpdate.getValue().longitude());

        simulation.publishConnectedLocations();
        assertTrue(distanceMeters(destination, simulation.currentPosition(driverId)) <= 50);
    }

    @Test
    void onlyCompletedTripsCanMoveTheSimulation() {
        assertThrows(IllegalArgumentException.class, () -> simulation.setCompletedTripDestination(
                driverId, "EN_CURSO", new GeoPosition(-27.4511, -58.9867)));
        verifyNoInteractions(m4);
    }

    @Test
    void assignedTripSuppressesAvailabilityAndCompletionRestoresIntentInTheM4LocationUpdate() {
        when(intents.findByDriverId(driverId)).thenReturn(Optional.of(connectedIntent));
        when(intents.findConnected()).thenReturn(List.of(connectedIntent));

        simulation.setOccupied(driverId, true);
        simulation.publishConnectedLocations();
        simulation.setCompletedTripDestination(driverId, "FINALIZADO", new GeoPosition(-27.4698, -58.8306));

        var sent = org.mockito.ArgumentCaptor.forClass(LocationUpdate.class);
        verify(m4, atLeast(2)).sendLocation(sent.capture());
        assertFalse(sent.getAllValues().get(0).available());
        assertFalse(sent.getAllValues().get(1).available());
        assertTrue(sent.getAllValues().get(sent.getAllValues().size() - 1).available());
    }

    private Driver approvedDriver() {
        Driver driver = mock(Driver.class);
        when(driver.getId()).thenReturn(driverId);
        when(driver.getQualificationStatus()).thenReturn(QualificationStatus.APPROVED);
        return driver;
    }

    private Vehicle approvedVehicle() {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(vehicleId);
        when(vehicle.getVehicleType()).thenReturn("AUTO");
        when(vehicle.getQualificationStatus()).thenReturn("APPROVED");
        return vehicle;
    }

    private static void assertWithinResistencia(GeoPosition position) {
        assertTrue(position.latitude() >= -27.60 && position.latitude() <= -27.32);
        assertTrue(position.longitude() >= -59.12 && position.longitude() <= -58.78);
    }

    private static double distanceMeters(GeoPosition first, GeoPosition second) {
        double north = (first.latitude() - second.latitude()) * 111_000;
        double east = (first.longitude() - second.longitude()) * 111_000
                * Math.cos(Math.toRadians(first.latitude()));
        return Math.hypot(north, east);
    }
}
