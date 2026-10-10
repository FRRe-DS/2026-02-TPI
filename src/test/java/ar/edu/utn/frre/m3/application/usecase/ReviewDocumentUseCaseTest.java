package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.port.out.AuditLogRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.DocumentRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.VehicleRepositoryPort;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import ar.edu.utn.frre.m3.domain.model.AuditLog;
import ar.edu.utn.frre.m3.domain.model.Document;
import ar.edu.utn.frre.m3.domain.model.DocumentKind;
import ar.edu.utn.frre.m3.domain.model.Driver;
import ar.edu.utn.frre.m3.domain.model.QualificationStatus;
import ar.edu.utn.frre.m3.domain.model.ReviewDecision;
import ar.edu.utn.frre.m3.domain.model.ReviewStatus;
import ar.edu.utn.frre.m3.domain.model.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewDocumentUseCaseTest {

    @Mock
    private DriverRepositoryPort driverRepositoryPort;

    @Mock
    private DocumentRepositoryPort documentRepositoryPort;

    @Mock
    private VehicleRepositoryPort vehicleRepositoryPort;

    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;

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
                driverRepositoryPort,
                documentRepositoryPort,
                vehicleRepositoryPort,
                auditLogRepositoryPort
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
        // Arrange
        long initialVersion = 7L;
        Driver driver = new Driver(
                driverId,
                "sub-auth0|12345",
                "Carlos Sainz",
                QualificationStatus.PENDING,
                initialVersion,
                createdAt,
                createdAt
        );

        Document document = new Document(
                documentId,
                driverId,
                null,
                DocumentKind.LICENSE,
                1,
                ReviewStatus.PENDING,
                issuedAt,
                validUntil,
                true,
                List.of(ServiceType.AUTO),
                initialVersion,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Licencia válida y verificada",
                initialVersion,
                "operator-admin",
                "test-correlation-id-001"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(driverRepositoryPort.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepositoryPort.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = reviewDocumentService.reviewDocument(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId);
        assertThat(result.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(result.getDriverVersion()).isEqualTo(8L);

        // Verify metadata immutability
        assertThat(result.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(result.getValidUntil()).isEqualTo(validUntil);
        assertThat(result.getKind()).isEqualTo(DocumentKind.LICENSE);
        assertThat(result.getRevision()).isEqualTo(1);
        assertThat(result.isCurrent()).isTrue();

        // Verify driver version incremented to 8
        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepositoryPort).save(driverCaptor.capture());
        assertThat(driverCaptor.getValue().getVersion()).isEqualTo(8L);

        // Verify audit log saved in same transaction
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepositoryPort).save(auditCaptor.capture());
        AuditLog savedAudit = auditCaptor.getValue();
        assertThat(savedAudit.getEntityType()).isEqualTo("DOCUMENT");
        assertThat(savedAudit.getEntityId()).isEqualTo(documentId);
        assertThat(savedAudit.getDriverId()).isEqualTo(driverId);
        assertThat(savedAudit.getAction()).isEqualTo("REVIEW");
        assertThat(savedAudit.getActor()).isEqualTo("operator-admin");
        assertThat(savedAudit.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(savedAudit.getNewStatus()).isEqualTo("APPROVED");
        assertThat(savedAudit.getReason()).isEqualTo("Licencia válida y verificada");
        assertThat(savedAudit.getCorrelationId()).isEqualTo("test-correlation-id-001");
    }

    @Test
    @DisplayName("Revisión exitosa: Documento de vehículo perteneciente al conductor")
    void shouldSuccessfullyReviewVehicleDocumentWhenVehicleBelongsToDriver() {
        // Arrange
        long initialVersion = 3L;
        Driver driver = new Driver(
                driverId,
                "sub-auth0|vehicle-driver",
                "Lewis Hamilton",
                QualificationStatus.PENDING,
                initialVersion,
                createdAt,
                createdAt
        );

        Document document = new Document(
                documentId,
                driverId,
                vehicleId,
                DocumentKind.INSURANCE,
                1,
                ReviewStatus.PENDING,
                issuedAt,
                validUntil,
                true,
                List.of(ServiceType.AUTO),
                initialVersion,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.OBSERVED,
                "Póliza ilegible en la cláusula de vigencia",
                initialVersion,
                "operator-reviewer",
                "corr-veh-01"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(driverRepositoryPort.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(vehicleRepositoryPort.existsByIdAndDriverId(vehicleId, driverId)).thenReturn(true);
        when(documentRepositoryPort.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Document result = reviewDocumentService.reviewDocument(command);

        // Assert
        assertThat(result.getReviewStatus()).isEqualTo(ReviewStatus.OBSERVED);
        assertThat(result.getDriverVersion()).isEqualTo(4L);
        assertThat(result.getVehicleId()).isEqualTo(vehicleId);

        verify(vehicleRepositoryPort).existsByIdAndDriverId(vehicleId, driverId);
        verify(auditLogRepositoryPort).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Conflicto de versión (412): Si If-Match no coincide con la versión del conductor lanza VersionMismatchException")
    void shouldThrowVersionMismatchExceptionWhenIfMatchDoesNotMatchDriverVersion() {
        // Arrange
        long currentVersion = 7L;
        long outdatedIfMatchVersion = 6L;

        Driver driver = new Driver(
                driverId,
                "sub-auth0|test",
                "Max Verstappen",
                QualificationStatus.APPROVED,
                currentVersion,
                createdAt,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Aprobado",
                outdatedIfMatchVersion,
                "operator",
                "corr-412"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));

        // Act & Assert
        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(VersionMismatchException.class)
                .hasMessageContaining("If-Match version mismatch: expected 6, but current version is 7");

        verify(driverRepositoryPort, never()).save(any());
        verify(documentRepositoryPort, never()).save(any());
        verify(auditLogRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Conductor no encontrado (404): Lanza DriverNotFoundException si el driverId no existe")
    void shouldThrowDriverNotFoundExceptionWhenDriverDoesNotExist() {
        // Arrange
        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Aprobado",
                1L,
                "operator",
                "corr-404-driver"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DriverNotFoundException.class)
                .hasMessageContaining(driverId.toString());

        verify(documentRepositoryPort, never()).findById(any());
        verify(auditLogRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Documento no encontrado (404): Lanza DocumentNotFoundException si el documentId no existe")
    void shouldThrowDocumentNotFoundExceptionWhenDocumentDoesNotExist() {
        // Arrange
        Driver driver = new Driver(
                driverId,
                "sub-auth0|test",
                "Fernando Alonso",
                QualificationStatus.PENDING,
                2L,
                createdAt,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Aprobado",
                2L,
                "operator",
                "corr-404-doc"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining(documentId.toString());

        verify(driverRepositoryPort, never()).save(any());
        verify(auditLogRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Pertenencia (404): Lanza DocumentNotFoundException si el documento pertenece a otro conductor")
    void shouldThrowDocumentNotFoundExceptionWhenDocumentBelongsToAnotherDriver() {
        // Arrange
        UUID otherDriverId = UUID.randomUUID();
        Driver driver = new Driver(
                driverId,
                "sub-auth0|driver-1",
                "Lando Norris",
                QualificationStatus.PENDING,
                1L,
                createdAt,
                createdAt
        );

        Document documentOfOtherDriver = new Document(
                documentId,
                otherDriverId, // Belongs to someone else!
                null,
                DocumentKind.LICENSE,
                1,
                ReviewStatus.PENDING,
                issuedAt,
                validUntil,
                true,
                List.of(ServiceType.AUTO),
                1L,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Aprobado",
                1L,
                "operator",
                "corr-belonging"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepositoryPort.findById(documentId)).thenReturn(Optional.of(documentOfOtherDriver));

        // Act & Assert
        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(driverRepositoryPort, never()).save(any());
        verify(documentRepositoryPort, never()).save(any());
        verify(auditLogRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Pertenencia (404): Lanza DocumentNotFoundException si el vehículo del documento no pertenece al conductor")
    void shouldThrowDocumentNotFoundExceptionWhenVehicleDoesNotBelongToDriver() {
        // Arrange
        Driver driver = new Driver(
                driverId,
                "sub-auth0|driver-1",
                "Charles Leclerc",
                QualificationStatus.PENDING,
                1L,
                createdAt,
                createdAt
        );

        Document vehicleDoc = new Document(
                documentId,
                driverId,
                vehicleId,
                DocumentKind.VEHICLE_REGISTRATION,
                1,
                ReviewStatus.PENDING,
                issuedAt,
                validUntil,
                true,
                List.of(ServiceType.AUTO),
                1L,
                createdAt
        );

        ReviewDocumentCommand command = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "Aprobado",
                1L,
                "operator",
                "corr-veh-mismatch"
        );

        when(driverRepositoryPort.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documentRepositoryPort.findById(documentId)).thenReturn(Optional.of(vehicleDoc));
        when(vehicleRepositoryPort.existsByIdAndDriverId(vehicleId, driverId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(command))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(driverRepositoryPort, never()).save(any());
        verify(documentRepositoryPort, never()).save(any());
        verify(auditLogRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Validación de regla de negocio: Motivo inválido (vacío o mayor a 500 caracteres) lanza DomainValidationException")
    void shouldThrowDomainValidationExceptionWhenReasonIsInvalid() {
        ReviewDocumentCommand emptyReasonCommand = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                "   ",
                1L,
                "operator",
                "corr-empty-reason"
        );

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(emptyReasonCommand))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Reason must be between 1 and 500 characters");

        String longReason = "a".repeat(501);
        ReviewDocumentCommand longReasonCommand = new ReviewDocumentCommand(
                driverId,
                documentId,
                ReviewDecision.APPROVED,
                longReason,
                1L,
                "operator",
                "corr-long-reason"
        );

        assertThatThrownBy(() -> reviewDocumentService.reviewDocument(longReasonCommand))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Reason must be between 1 and 500 characters");
    }
}
