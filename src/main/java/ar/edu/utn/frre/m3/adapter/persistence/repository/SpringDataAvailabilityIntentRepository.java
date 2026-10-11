package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.AvailabilityIntentJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AvailabilityIntentJpaEntity}.
 * Do not expose this repository outside the persistence adapter.
 */
public interface SpringDataAvailabilityIntentRepository
    extends JpaRepository<AvailabilityIntentJpaEntity, UUID> {

    Optional<AvailabilityIntentJpaEntity> findByDriverId(UUID driverId);
}
