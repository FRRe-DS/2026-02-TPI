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
        // Flush the old current revision before inserting its replacement: PostgreSQL
        // checks the partial unique index at each statement, not at transaction commit.
        return mapper.toDomain(repository.saveAndFlush(entity));
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

    @Override
    public Optional<Document> findCurrentLicenseByDriverId(UUID driverId) {
        return repository.findByDriverIdAndDocumentTypeAndVehicleIdIsNullAndCurrentTrue(driverId, DocumentType.LICENSE)
                .map(mapper::toDomain);
    }

    @Override
    public List<Document> findLicensesByDriverId(UUID driverId) {
        return repository.findByDriverIdAndDocumentTypeAndVehicleIdIsNullOrderByRevisionDesc(driverId, DocumentType.LICENSE)
                .stream().map(mapper::toDomain).toList();
    }
}
