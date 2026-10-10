package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterVehicleDocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private UnitOfWork unitOfWork;
    private RegisterVehicleDocumentService service;

    @BeforeEach
    void setUp() {
        // Direct transactional pass-through for unit testing
        unitOfWork = new UnitOfWork() {
            @Override
            public <T> T executeInTransaction(Supplier<T> action) {
                return action.get();
            }

            @Override
            public void executeInTransaction(Runnable action) {
                action.run();
            }
        };

        service = new RegisterVehicleDocumentService(documentRepository, vehicleRepository, unitOfWork);
    }

    @Test
    @DisplayName("Should successfully register initial revision (revision=1, isCurrent=true, status=PENDING)")
    void shouldRegisterInitialRevisionSuccessfully() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle(vehicleId, driverId);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findCurrentByVehicleIdAndType(vehicleId, DocumentType.INSURANCE))
            .thenReturn(Optional.empty());
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterVehicleDocumentCommand command = new RegisterVehicleDocumentCommand(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-12345",
            "San Cristobal Seguros",
            Instant.now().minus(2, ChronoUnit.DAYS),
            Instant.now().plus(180, ChronoUnit.DAYS),
            "storage://bucket/file.pdf"
        );

        Document created = service.registerVehicleDocument(command);

        assertNotNull(created);
        assertEquals(driverId, created.getDriverId());
        assertEquals(vehicleId, created.getVehicleId());
        assertEquals(DocumentType.INSURANCE, created.getDocumentType());
        assertEquals("POL-12345", created.getDocumentNumber());
        assertEquals(1, created.getRevision());
        assertTrue(created.isCurrent());
        assertEquals(DocumentStatus.PENDING, created.getStatus());

        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("Should supersede existing revision by setting previous to isCurrent=false and incrementing revision")
    void shouldSupersedeExistingRevision() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle(vehicleId, driverId);

        Document existingRevision = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-OLD-1",
            "San Cristobal",
            Instant.now().minus(30, ChronoUnit.DAYS),
            Instant.now().plus(10, ChronoUnit.DAYS),
            null,
            1
        );

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findCurrentByVehicleIdAndType(vehicleId, DocumentType.INSURANCE))
            .thenReturn(Optional.of(existingRevision));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterVehicleDocumentCommand command = new RegisterVehicleDocumentCommand(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-NEW-2",
            "San Cristobal",
            Instant.now(),
            Instant.now().plus(365, ChronoUnit.DAYS),
            null
        );

        Document newRevision = service.registerVehicleDocument(command);

        assertNotNull(newRevision);
        assertEquals(2, newRevision.getRevision());
        assertTrue(newRevision.isCurrent());
        assertEquals(DocumentStatus.PENDING, newRevision.getStatus());
        assertEquals("POL-NEW-2", newRevision.getDocumentNumber());

        // Verify previous revision was updated to isCurrent = false and saved
        assertFalse(existingRevision.isCurrent());
        verify(documentRepository, times(2)).save(any(Document.class));
    }

    @Test
    @DisplayName("Should throw VehicleNotFoundException when vehicleId does not exist (404)")
    void shouldThrowWhenVehicleNotFound() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        RegisterVehicleDocumentCommand command = new RegisterVehicleDocumentCommand(
            driverId,
            vehicleId,
            DocumentType.VEHICLE_REGISTRATION,
            "REG-999",
            "DNRPA",
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(365, ChronoUnit.DAYS),
            null
        );

        assertThrows(VehicleNotFoundException.class, () -> service.registerVehicleDocument(command));
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw VehicleForbiddenException when vehicle belongs to another driver (403)")
    void shouldThrowWhenVehicleBelongsToAnotherDriver() {
        UUID driverId = UUID.randomUUID();
        UUID ownerDriverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle(vehicleId, ownerDriverId);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        RegisterVehicleDocumentCommand command = new RegisterVehicleDocumentCommand(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-000",
            "Mapfre",
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(365, ChronoUnit.DAYS),
            null
        );

        assertThrows(VehicleForbiddenException.class, () -> service.registerVehicleDocument(command));
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should list documents for vehicle when driver is owner")
    void shouldListDocumentsWhenDriverIsOwner() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = new Vehicle(vehicleId, driverId);

        Document doc = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-1",
            "Sancor",
            Instant.now().minus(1, ChronoUnit.DAYS),
            Instant.now().plus(30, ChronoUnit.DAYS),
            null,
            1
        );

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findByVehicleIdOrderByRevisionDesc(vehicleId)).thenReturn(List.of(doc));

        List<Document> result = service.listVehicleDocuments(driverId, vehicleId);

        assertEquals(1, result.size());
        assertEquals("POL-1", result.get(0).getDocumentNumber());
    }
}

