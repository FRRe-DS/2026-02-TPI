package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.AuditLog;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.exception.DocumentNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReviewDocumentService implements ReviewDocumentUseCase {

    private final DriverRepository driverRepository;
    private final DocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditLogRepository auditLogRepository;

    public ReviewDocumentService(DriverRepository driverRepository,
                                 DocumentRepository documentRepository,
                                 VehicleRepository vehicleRepository,
                                 AuditLogRepository auditLogRepository) {
        this.driverRepository = Objects.requireNonNull(driverRepository, "driverRepository must not be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository must not be null");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository must not be null");
    }

    @Override
    @Transactional
    public Document reviewDocument(ReviewDocumentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Driver driver = driverRepository.findByIdWithPessimisticLock(command.driverId())
                .orElseThrow(() -> new DriverNotFoundException(command.driverId()));

        driver.validateVersion(command.ifMatchVersion());

        Document document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        if (!document.getDriverId().equals(command.driverId())) {
            throw new DocumentNotFoundException(command.documentId());
        }

        if (document.getVehicleId() != null
                && !vehicleRepository.existsByIdAndDriverId(document.getVehicleId(), command.driverId())) {
            throw new DocumentNotFoundException(command.documentId());
        }

        String previousStatus = document.getStatus().name();
        document.validateReview(command.decision(), command.reason());

        driver.incrementVersion();
        driverRepository.save(driver);

        document.review(command.decision(), command.reason(), command.actor(), driver.getVersion());
        Document updatedDocument = documentRepository.save(document);

        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                "DOCUMENT",
                updatedDocument.getId(),
                driver.getId(),
                "REVIEW",
                command.actor() != null && !command.actor().isBlank() ? command.actor() : "OPERATOR",
                previousStatus,
                updatedDocument.getStatus().name(),
                updatedDocument.getRejectionReason(),
                command.correlationId(),
                Instant.now()
        );
        auditLogRepository.save(auditLog);

        return updatedDocument;
    }
}
