package ar.edu.utn.frre.m3.adapter.persistence.mapper;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import ar.edu.utn.frre.m3.domain.Vehicle;
import org.springframework.stereotype.Component;

/**
 * Traduce entre la entidad de persistencia (VehicleJpaEntity) y el modelo de dominio (Vehicle).
 */
@Component
public class VehicleMapper {

    public Vehicle toDomain(VehicleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Vehicle(
            entity.getId(),
            entity.getDriverId(),
            entity.getVehicleType(),
            entity.getServiceTypes(),
            entity.getCountryCode() != null ? entity.getCountryCode() : "AR",
            entity.getPlate(),
            entity.getMake(),
            entity.getModel(),
            entity.getModelYear(),
            entity.getQualificationStatus(),
            entity.getDriverVersion(),
            entity.getCreatedAt()
        );
    }
}