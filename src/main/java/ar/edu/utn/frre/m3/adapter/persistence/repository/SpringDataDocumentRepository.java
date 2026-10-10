package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.domain.DocumentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for DocumentJpaEntity.
 */
public interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    Optional<DocumentJpaEntity> findByVehicleIdAndDocumentTypeAndCurrentTrue(UUID vehicleId, DocumentType documentType);

    List<DocumentJpaEntity> findByVehicleIdOrderByRevisionDesc(UUID vehicleId);
}
