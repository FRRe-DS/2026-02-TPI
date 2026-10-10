package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentCommand;
import ar.edu.utn.frre.m3.application.port.in.ReviewDocumentUseCase;
import ar.edu.utn.frre.m3.application.port.out.AuditLogRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.DocumentRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.application.port.out.VehicleRepositoryPort;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DomainValidationException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.model.AuditLog;
import ar.edu.utn.frre.m3.domain.model.Document;
import ar.edu.utn.frre.m3.domain.model.Driver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReviewDocumentService implements ReviewDocumentUseCase {

    private final DriverRepositoryPort driverRepositoryPort;
    private final DocumentRepositoryPort documentRepositoryPort;
    private final VehicleRepositoryPort vehicleRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;

    public ReviewDocumentService(DriverRepositoryPort driverRepositoryPort,
                                 DocumentRepositoryPort documentRepositoryPort,
                                 VehicleRepositoryPort vehicleRepositoryPort,
                                 AuditLogRepositoryPort auditLogRepositoryPort) {
        this.driverRepositoryPort = Objects.requireNonNull(driverRepositoryPort, "driverRepositoryPort must not be null");
        this.documentRepositoryPort = Objects.requireNonNull(documentRepositoryPort, "documentRepositoryPort must not be null");
        this.vehicleRepositoryPort = Objects.requireNonNull(vehicleRepositoryPort, "vehicleRepositoryPort must not be null");
        this.auditLogRepositoryPort = Objects.requireNonNull(auditLogRepositoryPort, "auditLogRepositoryPort must not be null");
    }

    @Override
    @Transactional
    public Document reviewDocument(ReviewDocumentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.reason() == null || command.reason().trim().isEmpty() || command.reason().length() > 500) {
            throw new DomainValidationException("Reason must be between 1 and 500 characters");
        }
        if (command.decision() == null) {
            throw new DomainValidationException("Decision must not be null");
        }

        // 1. Lock driver pessimistically to serialize updates and prevent race conditions
        Driver driver = driverRepositoryPort.findByIdWithPessimisticLock(command.driverId())
                .orElseThrow(() -> new DriverNotFoundException(command.driverId()));

        // 2. Validate If-Match version against current driver.version (412 on mismatch)
        driver.validateVersion(command.ifMatchVersion());

        // 3. Find document and verify ownership (Pertenencia: 404 if not found or mismatch)
        Document document = documentRepositoryPort.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        if (!document.getDriverId().equals(command.driverId())) {
            throw new DocumentNotFoundException(command.documentId());
        }

        if (document.getVehicleId() != null
                && !vehicleRepositoryPort.existsByIdAndDriverId(document.getVehicleId(), command.driverId())) {
            throw new DocumentNotFoundException(command.documentId());
        }

        String previousStatus = document.getReviewStatus().name();

        // 4. Increment driver version
        driver.incrementVersion();
        driver = driverRepositoryPort.save(driver);

        // 5. Update document review status and driverVersion (metadata dates remain immutable)
        document.review(command.decision(), driver.getVersion());
        Document updatedDocument = documentRepositoryPort.save(document);

        // 6. Record audit log within the same transaction
        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                "DOCUMENT",
                updatedDocument.getId(),
                driver.getId(),
                "REVIEW",
                command.actor() != null && !command.actor().isBlank() ? command.actor() : "OPERATOR",
                previousStatus,
                updatedDocument.getReviewStatus().name(),
                command.reason(),
                command.correlationId(),
                Instant.now()
        );
        auditLogRepositoryPort.save(auditLog);

        return updatedDocument;
    }
}
