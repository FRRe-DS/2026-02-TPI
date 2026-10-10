package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleForbiddenException;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service implementing RegisterVehicleDocumentUseCase (HU-06 / RF-3.3).
 * Operates purely on domain models and ports, executed within UnitOfWork transactional boundaries.
 */
public class RegisterVehicleDocumentService implements RegisterVehicleDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final VehicleRepository vehicleRepository;
    private final UnitOfWork unitOfWork;

    public RegisterVehicleDocumentService(
        DocumentRepository documentRepository,
        VehicleRepository vehicleRepository,
        UnitOfWork unitOfWork
    ) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository cannot be null");
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository, "vehicleRepository cannot be null");
        this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork cannot be null");
    }

    @Override
    public Document registerVehicleDocument(RegisterVehicleDocumentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");

        return unitOfWork.executeInTransaction(() -> {
            // 1. Verify vehicle existence (404 if not found)
            Vehicle vehicle = vehicleRepository.findById(command.vehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(command.vehicleId()));

            // 2. Verify vehicle ownership (403 if belonging to another driver)
            if (!vehicle.belongsToDriver(command.driverId())) {
                throw new VehicleForbiddenException(command.driverId(), command.vehicleId());
            }

            // 3. Find existing current document for this vehicle and type
            Optional<Document> existingCurrent = documentRepository.findCurrentByVehicleIdAndType(
                command.vehicleId(),
                command.documentType()
            );

            // 4. Calculate next revision number
            int nextRevision = existingCurrent.map(doc -> doc.getRevision() + 1).orElse(1);

            // 5. Non-destructive versioning: Mark previous current revision as non-current
            existingCurrent.ifPresent(previous -> {
                previous.markAsNonCurrent();
                documentRepository.save(previous);
            });

            // 6. Instantiate new document revision (domain model validates issuedAt < validUntil, sets isCurrent=true, status=PENDING)
            Document newDocument = Document.createPendingRevision(
                command.driverId(),
                command.vehicleId(),
                command.documentType(),
                command.documentNumber(),
                command.issuingEntity(),
                command.issuedAt(),
                command.validUntil(),
                command.storageReference(),
                nextRevision
            );

            // 7. Persist and return
            return documentRepository.save(newDocument);
        });
    }

    @Override
    public List<Document> listVehicleDocuments(UUID driverId, UUID vehicleId) {
        Objects.requireNonNull(driverId, "driverId cannot be null");
        Objects.requireNonNull(vehicleId, "vehicleId cannot be null");

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new VehicleNotFoundException(vehicleId));

        if (!vehicle.belongsToDriver(driverId)) {
            throw new VehicleForbiddenException(driverId, vehicleId);
        }

        return documentRepository.findByVehicleIdOrderByRevisionDesc(vehicleId);
    }
}

