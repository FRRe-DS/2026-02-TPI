package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for VehicleJpaEntity.
 */
public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    List<VehicleJpaEntity> findByDriverIdOrderByCreatedAtDesc(UUID driverId);

    Optional<VehicleJpaEntity> findByIdAndDriverId(UUID id, UUID driverId);

    boolean existsByIdAndDriverId(UUID id, UUID driverId);

    java.util.List<VehicleJpaEntity> findByDriverId(UUID driverId);
}
