package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.mapper.DocumentMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDocumentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter implementing the DocumentRepository application output port.
 */
@Repository
public class DocumentPersistenceAdapter implements DocumentRepository {

    private final SpringDataDocumentRepository springDataRepository;
    private final DocumentMapper documentMapper;

    public DocumentPersistenceAdapter(
        SpringDataDocumentRepository springDataRepository,
        DocumentMapper documentMapper
    ) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
        this.documentMapper = Objects.requireNonNull(documentMapper, "documentMapper cannot be null");
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = documentMapper.toJpaEntity(document);
        DocumentJpaEntity saved = springDataRepository.save(entity);
        return documentMapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findCurrentByVehicleIdAndType(UUID vehicleId, DocumentType documentType) {
        return springDataRepository.findByVehicleIdAndDocumentTypeAndCurrentTrue(vehicleId, documentType)
            .map(documentMapper::toDomain);
    }

    @Override
    public List<Document> findByVehicleIdOrderByRevisionDesc(UUID vehicleId) {
        return springDataRepository.findByVehicleIdOrderByRevisionDesc(vehicleId).stream()
            .map(documentMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return springDataRepository.findById(id)
            .map(documentMapper::toDomain);
    }
}

