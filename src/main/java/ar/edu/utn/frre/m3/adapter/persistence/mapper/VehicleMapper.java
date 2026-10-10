package ar.edu.utn.frre.m3.adapter.persistence.mapper;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import ar.edu.utn.frre.m3.domain.Vehicle;
import org.springframework.stereotype.Component;

/**
 * Maps between Vehicle domain model and VehicleJpaEntity.
 */
@Component
public class VehicleMapper {

    public Vehicle toDomain(VehicleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Vehicle(entity.getId(), entity.getDriverId());
    }
}

