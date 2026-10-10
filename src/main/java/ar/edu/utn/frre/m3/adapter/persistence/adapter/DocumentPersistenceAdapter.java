package ar.edu.utn.frre.m3.adapter.persistence.adapter;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDocumentRepository;
import ar.edu.utn.frre.m3.application.port.out.DocumentRepositoryPort;
import ar.edu.utn.frre.m3.domain.model.Document;
import ar.edu.utn.frre.m3.domain.model.DocumentKind;
import ar.edu.utn.frre.m3.domain.model.ReviewStatus;
import ar.edu.utn.frre.m3.domain.model.ServiceType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DocumentPersistenceAdapter implements DocumentRepositoryPort {

    private final SpringDataDocumentRepository documentRepository;

    public DocumentPersistenceAdapter(SpringDataDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
        return documentRepository.findById(documentId)
                .map(this::toDomain);
    }

    @Override
    public Document save(Document document) {
        DocumentJpaEntity entity = toEntity(document);
        DocumentJpaEntity saved = documentRepository.save(entity);
        return toDomain(saved);
    }

    private Document toDomain(DocumentJpaEntity entity) {
        List<ServiceType> serviceTypes = Collections.emptyList();
        if (entity.getServiceTypes() != null && !entity.getServiceTypes().isBlank()) {
            serviceTypes = Arrays.stream(entity.getServiceTypes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(ServiceType::valueOf)
                    .toList();
        }

        return new Document(
                entity.getId(),
                entity.getDriverId(),
                entity.getVehicleId(),
                DocumentKind.valueOf(entity.getKind()),
                entity.getRevision(),
                ReviewStatus.valueOf(entity.getReviewStatus()),
                entity.getIssuedAt(),
                entity.getValidUntil(),
                entity.getIsCurrent(),
                serviceTypes,
                entity.getDriverVersion(),
                entity.getCreatedAt()
        );
    }

    private DocumentJpaEntity toEntity(Document domain) {
        String serviceTypesStr = null;
        if (domain.getServiceTypes() != null && !domain.getServiceTypes().isEmpty()) {
            serviceTypesStr = domain.getServiceTypes().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
        }

        return new DocumentJpaEntity(
                domain.getId(),
                domain.getDriverId(),
                domain.getVehicleId(),
                domain.getKind().name(),
                domain.getRevision(),
                domain.getReviewStatus().name(),
                domain.getIssuedAt(),
                domain.getValidUntil(),
                domain.isCurrent(),
                serviceTypesStr,
                domain.getDriverVersion(),
                domain.getCreatedAt()
        );
    }
}
