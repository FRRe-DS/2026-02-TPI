package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.DriverOperationalState;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de sólo lectura que M4 hace a M3 para validar candidatos (contrato M3 2.0.0):
 * estado operativo, última ubicación y vehículo del conductor.
 *
 * <p>No modifica datos. {@code DISPONIBLE} se informa sólo si en este momento se cumplen las mismas
 * condiciones que exige {@link RequestAvailabilityService} para conectarse: conductor aprobado,
 * vehículo propio aprobado y licencia, seguro y cédula actuales, aprobados y vigentes. Si alguna dejó
 * de cumplirse después de conectarse (por ejemplo venció el seguro), se informa {@code CONECTADO}.
 */
public class DriverOperationalStatusService {

    public record Status(UUID driverId, DriverOperationalState state, UUID activeVehicleId, Instant updatedAt, long driverVersion) {}
    public record LastLocation(UUID driverId, Double latitude, Double longitude, Instant recordedAt) {}
    public record VehicleView(Vehicle vehicle, List<Document> currentDocuments) {}

    private final DriverRepository drivers;
    private final AvailabilityIntentRepository intents;
    private final VehicleRepository vehicles;
    private final DocumentRepository documents;
    private final LocationSimulationService locations;
    private final Clock clock;

    public DriverOperationalStatusService(DriverRepository drivers, AvailabilityIntentRepository intents,
            VehicleRepository vehicles, DocumentRepository documents, LocationSimulationService locations, Clock clock) {
        this.drivers = Objects.requireNonNull(drivers);
        this.intents = Objects.requireNonNull(intents);
        this.vehicles = Objects.requireNonNull(vehicles);
        this.documents = Objects.requireNonNull(documents);
        this.locations = Objects.requireNonNull(locations);
        this.clock = Objects.requireNonNull(clock);
    }

    public Status status(UUID driverId) {
        Driver driver = requireDriver(driverId);
        AvailabilityIntent intent = intents.findByDriverId(driverId).orElse(null);
        if (intent == null) {
            Instant lastChange = driver.getUpdatedAt() != null ? driver.getUpdatedAt()
                    : driver.getCreatedAt() != null ? driver.getCreatedAt() : clock.instant();
            return new Status(driverId, DriverOperationalState.OFFLINE, null, lastChange, driver.getVersion());
        }
        return new Status(driverId, stateOf(driver, intent), intent.getVehicleId(), intent.getUpdatedAt(), driver.getVersion());
    }

    public LastLocation location(UUID driverId) {
        requireDriver(driverId);
        var snapshot = locations.currentLocation(driverId);
        if (snapshot == null) return new LastLocation(driverId, null, null, null);
        return new LastLocation(driverId, snapshot.latitude(), snapshot.longitude(), snapshot.recordedAt().toInstant());
    }

    public VehicleView vehicle(UUID driverId, UUID vehicleId) {
        requireDriver(driverId);
        Vehicle vehicle = vehicles.findByIdAndDriverId(vehicleId, driverId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        List<Document> current = List.of(DocumentType.INSURANCE, DocumentType.VEHICLE_REGISTRATION).stream()
                .map(type -> documents.findCurrentByVehicleIdAndType(vehicleId, type))
                .flatMap(Optional::stream)
                .toList();
        return new VehicleView(vehicle, current);
    }

    private DriverOperationalState stateOf(Driver driver, AvailabilityIntent intent) {
        if (!intent.isConnected()) return DriverOperationalState.OFFLINE;
        if (locations.isOccupied(driver.getId())) return DriverOperationalState.BUSY;
        if (intent.isAvailable() && qualifiedToDispatch(driver, intent.getVehicleId())) return DriverOperationalState.AVAILABLE;
        return DriverOperationalState.CONNECTED;
    }

    private boolean qualifiedToDispatch(Driver driver, UUID vehicleId) {
        if (!driver.isOperable()) return false;
        Vehicle vehicle = vehicles.findByIdAndDriverId(vehicleId, driver.getId()).orElse(null);
        if (vehicle == null || !"APPROVED".equals(vehicle.getQualificationStatus())) return false;
        Instant now = clock.instant();
        try {
            QualificationService.requireValid(documents.findCurrentLicenseByDriverId(driver.getId()).orElse(null), now, "licencia");
            QualificationService.requireValid(documents.findCurrentByVehicleIdAndType(vehicleId, DocumentType.INSURANCE).orElse(null), now, "seguro");
            QualificationService.requireValid(documents.findCurrentByVehicleIdAndType(vehicleId, DocumentType.VEHICLE_REGISTRATION).orElse(null), now, "cédula");
            return true;
        } catch (DomainValidationException notCurrent) {
            return false;
        }
    }

    private Driver requireDriver(UUID driverId) {
        return drivers.findById(driverId).orElseThrow(() -> DriverNotFoundException.forId(driverId));
    }
}
