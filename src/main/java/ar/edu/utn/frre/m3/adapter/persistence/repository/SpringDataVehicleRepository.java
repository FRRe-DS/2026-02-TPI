package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for VehicleJpaEntity.
 */
public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    boolean existsByIdAndDriverId(UUID id, UUID driverId);
}
