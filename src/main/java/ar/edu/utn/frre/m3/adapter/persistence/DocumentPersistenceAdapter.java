package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.mapper.DocumentMapper;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDocumentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentPersistenceAdapter implements DocumentRepository {

    private final SpringDataDocumentRepository repository;
    private final DocumentMapper mapper;

    public DocumentPersistenceAdapter(SpringDataDocumentRepository repository, DocumentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = mapper.toJpaEntity(document);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Document> findCurrentByVehicleIdAndType(UUID vehicleId, DocumentType documentType) {
        return repository.findByVehicleIdAndDocumentTypeAndCurrentTrue(vehicleId, documentType)
                .map(mapper::toDomain);
    }

    @Override
    public List<Document> findByVehicleIdOrderByRevisionDesc(UUID vehicleId) {
        return repository.findByVehicleIdOrderByRevisionDesc(vehicleId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
