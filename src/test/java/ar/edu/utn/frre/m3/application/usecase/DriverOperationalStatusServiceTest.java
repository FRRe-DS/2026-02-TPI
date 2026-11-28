package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** T7 · Qué estado operativo informa M3 a M4 según la intención de Ana y su habilitación vigente. */
class DriverOperationalStatusServiceTest {

    static final Instant NOW = Instant.parse("2026-11-28T12:00:00Z");
    DriverRepository drivers; AvailabilityIntentRepository intents; VehicleRepository vehicles;
    DocumentRepository documents; LocationSimulationService locations;
    DriverOperationalStatusService service;
    UUID driverId = UUID.randomUUID(), vehicleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        drivers = mock(DriverRepository.class); intents = mock(AvailabilityIntentRepository.class);
        vehicles = mock(VehicleRepository.class); documents = mock(DocumentRepository.class);
        locations = mock(LocationSimulationService.class);
        service = new DriverOperationalStatusService(drivers, intents, vehicles, documents, locations, Clock.fixed(NOW, ZoneOffset.UTC));
        anaIs(QualificationStatus.APPROVED);
        carIs("APPROVED");
        documentsValidUntil(NOW.plusSeconds(86400));
    }

    void anaIs(QualificationStatus status) {
        when(drivers.findById(driverId)).thenReturn(Optional.of(
            new Driver(driverId, "ana", "Ana", status, 4, NOW.minusSeconds(3600), NOW.minusSeconds(60))));
    }
    void carIs(String status) {
        when(vehicles.findByIdAndDriverId(vehicleId, driverId)).thenReturn(Optional.of(new Vehicle(vehicleId, driverId, "AUTO",
            Set.of("AUTO"), "AR", "AA123BB", "Fiat", "Cronos", 2023, status, 1L, NOW, null, null, Set.of("STANDARD"))));
    }
    void documentsValidUntil(Instant until) {
        var license = Document.createPendingLicense(driverId, "LIC", NOW.minusSeconds(86400), until, Set.of(ServiceType.AUTO), null, 1, 1);
        license.review(ReviewDecision.APPROVED, "ok", "operator", 1);
        when(documents.findCurrentLicenseByDriverId(driverId)).thenReturn(Optional.of(license));
        for (var type : new DocumentType[]{DocumentType.INSURANCE, DocumentType.VEHICLE_REGISTRATION}) {
            var doc = Document.createPendingRevision(driverId, vehicleId, type, "DOC", "Emisor", NOW.minusSeconds(86400), until, null, 1);
            doc.review(ReviewDecision.APPROVED, "ok", "operator", 1);
            when(documents.findCurrentByVehicleIdAndType(vehicleId, type)).thenReturn(Optional.of(doc));
        }
    }
    void anaRequested(boolean connected, boolean available) {
        when(intents.findByDriverId(driverId)).thenReturn(Optional.of(new AvailabilityIntent(UUID.randomUUID(), driverId, vehicleId,
            connected, available, IntentStatus.APPLIED, 2, 2L, NOW.minusSeconds(600), NOW.minusSeconds(30), NOW.minusSeconds(29))));
    }

    @Test @DisplayName("Sin intención registrada: DESCONECTADO y sin vehículo activo")
    void withoutIntentIsOffline() {
        var status = service.status(driverId);
        assertEquals(DriverOperationalState.OFFLINE, status.state());
        assertNull(status.activeVehicleId());
        assertEquals(NOW.minusSeconds(60), status.updatedAt());
        assertEquals(4, status.driverVersion());
    }

    @Test @DisplayName("connected=false: DESCONECTADO aunque tenga vehículo elegido")
    void disconnectedIntentIsOffline() {
        anaRequested(false, false);
        assertEquals(DriverOperationalState.OFFLINE, service.status(driverId).state());
    }

    @Test @DisplayName("Conectada en pausa: CONECTADO")
    void connectedButPausedIsConnected() {
        anaRequested(true, false);
        assertEquals(DriverOperationalState.CONNECTED, service.status(driverId).state());
    }

    @Test @DisplayName("Conectada, acepta viajes y todo vigente: DISPONIBLE con su vehículo")
    void qualifiedAndAvailableIsAvailable() {
        anaRequested(true, true);
        var status = service.status(driverId);
        assertEquals(DriverOperationalState.AVAILABLE, status.state());
        assertEquals(vehicleId, status.activeVehicleId());
        assertEquals(NOW.minusSeconds(30), status.updatedAt());
    }

    @Test @DisplayName("Suspendieron el auto después de conectarse: baja a CONECTADO")
    void suspendedVehicleDowngradesToConnected() {
        anaRequested(true, true);
        carIs("SUSPENDED");
        assertEquals(DriverOperationalState.CONNECTED, service.status(driverId).state());
    }

    @Test @DisplayName("Suspendieron a Ana: baja a CONECTADO")
    void suspendedDriverDowngradesToConnected() {
        anaRequested(true, true);
        anaIs(QualificationStatus.SUSPENDED);
        assertEquals(DriverOperationalState.CONNECTED, service.status(driverId).state());
    }

    @Test @DisplayName("Venció el seguro (vigente hasta ahora mismo): baja a CONECTADO")
    void expiredDocumentDowngradesToConnected() {
        anaRequested(true, true);
        documentsValidUntil(NOW);
        assertEquals(DriverOperationalState.CONNECTED, service.status(driverId).state());
    }

    @Test @DisplayName("Con viaje en curso: OCUPADO, aunque haya pedido disponible")
    void occupiedDriverIsBusy() {
        anaRequested(true, true);
        when(locations.isOccupied(driverId)).thenReturn(true);
        assertEquals(DriverOperationalState.BUSY, service.status(driverId).state());
    }

    @Test @DisplayName("Ubicación desconocida: tres nulos, nunca (0, 0)")
    void unknownLocationIsNull() {
        var location = service.location(driverId);
        assertNull(location.latitude()); assertNull(location.longitude()); assertNull(location.recordedAt());
    }

    @Test @DisplayName("Ubicación conocida: coordenadas y momento del registro")
    void knownLocationIsReturned() {
        when(locations.currentLocation(driverId)).thenReturn(new LocationSimulationService.LocationSnapshot(
            driverId, -27.4511, -58.9867, OffsetDateTime.parse("2026-11-28T08:59:00-03:00"), true));
        var location = service.location(driverId);
        assertEquals(-27.4511, location.latitude());
        assertEquals(Instant.parse("2026-11-28T11:59:00Z"), location.recordedAt());
    }

    @Test @DisplayName("Vehículo ajeno o inexistente: 404 de vehículo; conductor inexistente: 404 de conductor")
    void missingVehicleOrDriverIsNotFound() {
        when(vehicles.findByIdAndDriverId(any(), any())).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> service.vehicle(driverId, UUID.randomUUID()));
        UUID unknown = UUID.randomUUID();
        when(drivers.findById(unknown)).thenReturn(Optional.empty());
        assertThrows(DriverNotFoundException.class, () -> service.status(unknown));
    }
}
