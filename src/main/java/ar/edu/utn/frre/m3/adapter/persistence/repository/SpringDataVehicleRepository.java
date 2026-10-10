package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, UUID> {
    boolean existsByIdAndDriverId(UUID id, UUID driverId);
}
