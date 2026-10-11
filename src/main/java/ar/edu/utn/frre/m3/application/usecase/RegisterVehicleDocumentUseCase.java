package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.Document;
import java.util.List;
import java.util.UUID;

/**
 * Use case input port for registering and querying vehicle documents (HU-06).
 */
public interface RegisterVehicleDocumentUseCase {

    /**
     * Registers a new revision of a vehicle document.
     * Previous current document of the same type is marked non-current,
     * and the new document is saved as current with status PENDING.
     */
    Document registerVehicleDocument(RegisterVehicleDocumentCommand command);

    /**
     * Lists the document revision history for a vehicle belonging to the specified driver.
     */
    List<Document> listVehicleDocuments(UUID driverId, UUID vehicleId);
}

