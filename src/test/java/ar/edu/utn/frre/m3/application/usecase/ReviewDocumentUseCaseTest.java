package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewDocumentUseCaseTest {

    @Mock private DriverRepository driverRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private ReviewDocumentService reviewDocumentService;

    private UUID driverId;
    private UUID documentId;
    private UUID vehicleId;
    private Instant issuedAt;
    private Instant validUntil;
    private Instant createdAt;

    @BeforeEach
    void setUp() {
        reviewDocumentService = new ReviewDocumentService(
                driverRepository,
                documentRepository,
                vehicleRepository,
                auditLogRepository
        );

        driverId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        createdAt = Instant.now().minus(5, ChronoUnit.DAYS);
        issuedAt = Instant.now().minus(30, ChronoUnit.DAYS);
        validUntil = Instant.now().plus(365, ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("Revisión exitosa: Conductor incrementa versión, documento se aprueba y se guarda auditoría")
    void shouldSuccessfullyReviewDriverDocumentAndIncrementVersionAndSaveAuditLog() {
        long initialVersion = 7L;
        Driver driver = new Driver(driverId, "sub|123", "Carlos Sainz", QualificationStatus.PENDING, initialVersion, createdAt, createdAt);
        Document document = new Document(documentId, driverId, null, DocumentType.LICENSE, "LIC-123", "DGT", issuedAt, validUntil, null, 1, DocumentStatus.PENDING, true, initialVersion, null, null, null, createdAt);

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Licencia válida y verificada", initialVersion, "operator-admin", "test-corr-001"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        Document result = reviewDocumentService.reviewDocument(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(result.getDriverVersion()).isEqualTo(8L);

        assertThat(result.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(result.getValidUntil()).isEqualTo(validUntil);
        assertThat(result.getDocumentNumber()).isEqualTo("LIC-123");

        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository).save(driverCaptor.capture());
        assertThat(driverCaptor.getValue().getVersion()).isEqualTo(8L);

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getEntityType()).isEqualTo("DOCUMENT");
        assertThat(savedAudit.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(savedAudit.getNewStatus()).isEqualTo("APPROVED");
        assertThat(savedAudit.getReason()).isEqualTo("Licencia válida y verificada");
    }

    @Test
    @DisplayName("Revisión exitosa: Documento de vehículo perteneciente al conductor")
    void shouldSuccessfullyReviewVehicleDocumentWhenVehicleBelongsToDriver() {
        long initialVersion = 3L;
        Driver driver = new Driver(driverId, "sub|veh", "Lewis Hamilton", QualificationStatus.PENDING, initialVersion, createdAt, createdAt);
        Document document = new Document(documentId, driverId, vehicleId, DocumentType.INSURANCE, "POL-1", "Aseguradora", issuedAt, validUntil, null, 1, DocumentStatus.PENDING, true, initialVersion, null, null, null, createdAt);

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.OBSERVED, "Póliza ilegible", initialVersion, "operator-rev", "corr-veh-01"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(vehicleRepository.existsByIdAndDriverId(vehicleId, driverId)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

        Document result = reviewDocumentService.reviewDocument(command);

        assertThat(result.getStatus()).isEqualTo(DocumentStatus.OBSERVED);
        assertThat(result.getDriverVersion()).isEqualTo(4L);
        assertThat(result.getVehicleId()).isEqualTo(vehicleId);

        verify(vehicleRepository).existsByIdAndDriverId(vehicleId, driverId);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Conflicto de versión (412): If-Match no coincide con la versión actual")
    void shouldThrowVersionMismatchExceptionWhenIfMatchDoesNotMatchDriverVersion() {
        Driver driver = new Driver(driverId, "sub|test", "Max Verstappen", QualificationStatus.APPROVED, 7L, createdAt, createdAt);
        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Aprobado", 6L, "operator", "corr-412"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessageContaining("If-Match version mismatch: expected 6, but current version is 7");

        verify(documentRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conductor no encontrado (404)")
    void shouldThrowDriverNotFoundExceptionWhenDriverDoesNotExist() {
        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Aprobado", 1L, "operator", "corr-404"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DriverNotFoundException.class);

        verify(documentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Documento no encontrado (404)")
    void shouldThrowDocumentNotFoundExceptionWhenDocumentDoesNotExist() {
        Driver driver = new Driver(driverId, "sub|1", "Fernando Alonso", QualificationStatus.PENDING, 2L, createdAt, createdAt);
        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Aprobado", 2L, "operator", "corr-404-doc"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("Pertenencia (404): Falla si el documento pertenece a otro conductor")
    void shouldThrowDocumentNotFoundExceptionWhenDocumentBelongsToAnotherDriver() {
        UUID otherDriverId = UUID.randomUUID();
        Driver driver = new Driver(driverId, "sub|1", "Lando Norris", QualificationStatus.PENDING, 1L, createdAt, createdAt);
        Document otherDoc = new Document(documentId, otherDriverId, null, DocumentType.LICENSE, "LIC-99", "DGT", issuedAt, validUntil, null, 1, DocumentStatus.PENDING, true, 1L, null, null, null, createdAt);

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Aprobado", 1L, "operator", "corr-belonging"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(otherDoc));

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Pertenencia (404): Falla si el vehículo del documento no pertenece al conductor")
    void shouldThrowDocumentNotFoundExceptionWhenVehicleDoesNotBelongToDriver() {
        Driver driver = new Driver(driverId, "sub|1", "Charles Leclerc", QualificationStatus.PENDING, 1L, createdAt, createdAt);
        Document vehicleDoc = new Document(documentId, driverId, vehicleId, DocumentType.VEHICLE_REGISTRATION, "REG-1", "DNRPA", issuedAt, validUntil, null, 1, DocumentStatus.PENDING, true, 1L, null, null, null, createdAt);

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "Aprobado", 1L, "operator", "corr-veh"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(vehicleDoc));
        when(vehicleRepository.existsByIdAndDriverId(vehicleId, driverId)).thenReturn(false);

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("Validación de regla de negocio: Motivo inválido lanza DomainValidationException")
    void shouldThrowDomainValidationExceptionWhenReasonIsInvalid() {
        Driver driver = new Driver(driverId, "sub|1", "Driver", QualificationStatus.PENDING, 1L, createdAt, createdAt);
        Document doc = new Document(documentId, driverId, null, DocumentType.LICENSE, "LIC-1", "DGT", issuedAt, validUntil, null, 1, DocumentStatus.PENDING, true, 1L, null, null, null, createdAt);

        ReviewDocumentCommand emptyReasonCommand = new ReviewDocumentCommand(
                driverId, documentId, ReviewDecision.APPROVED, "   ", 1L, "operator", "corr-blank"
        );

        when(driverRepository.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(emptyReasonCommand))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Reason must be between 1 and 500 characters");
    }
}