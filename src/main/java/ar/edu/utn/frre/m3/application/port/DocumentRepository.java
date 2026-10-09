package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Document persistence.
 */
public interface DocumentRepository {

    Document save(Document document);

    Optional<Document> findCurrentByVehicleIdAndType(UUID vehicleId, DocumentType documentType);

    List<Document> findByVehicleIdOrderByRevisionDesc(UUID vehicleId);

    Optional<Document> findById(UUID id);
}

