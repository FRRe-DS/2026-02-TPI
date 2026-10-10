package ar.edu.utn.frre.m3.adapter.persistence.mapper;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.domain.Document;
import org.springframework.stereotype.Component;

/**
 * Maps between Document domain model and DocumentJpaEntity.
 */
@Component
public class DocumentMapper {

    public Document toDomain(DocumentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Document.reconstitute(
            entity.getId(),
            entity.getDriverId(),
            entity.getVehicleId(),
            entity.getDocumentType(),
            entity.getDocumentNumber(),
            entity.getIssuingEntity(),
            entity.getIssuedAt(),
            entity.getValidUntil(),
            entity.getStorageReference(),
            entity.getRevision(),
            entity.getStatus(),
            entity.isCurrent(),
            entity.getCreatedAt()
        );
    }

    public DocumentJpaEntity toJpaEntity(Document domain) {
        if (domain == null) {
            return null;
        }
        return new DocumentJpaEntity(
            domain.getId(),
            domain.getDriverId(),
            domain.getVehicleId(),
            domain.getDocumentType(),
            domain.getDocumentNumber(),
            domain.getIssuingEntity(),
            domain.getIssuedAt(),
            domain.getValidUntil(),
            domain.getStorageReference(),
            domain.getRevision(),
            domain.getStatus(),
            domain.isCurrent(),
            domain.getCreatedAt()
        );
    }
}

