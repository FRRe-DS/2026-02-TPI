package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing {@link DriverRepository}.
 *
 * <p>Checks driver existence and qualification using the existing {@code vehicles}
 * and related tables from V1 migration.
 *
 * <p>NOTE: This is a stub implementation that can be replaced when the full
 * {@code drivers} table migration is added. For now it checks vehicle ownership.
 */
@Component
public class DriverPersistenceAdapter implements DriverRepository {

    private final SpringDataVehicleRepository vehicleRepository;

    public DriverPersistenceAdapter(SpringDataVehicleRepository vehicleRepository) {
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository);
    }

    /**
     * A driver exists if they have at least one vehicle registered in M3.
     * This will be replaced with a dedicated drivers table query in a future migration.
     */
    @Override
    public boolean existsById(UUID driverId) {
        return vehicleRepository.existsByDriverId(driverId);
    }

    /**
     * Stub: always returns true for known drivers.
     * Replace with: SELECT qualification_status FROM drivers WHERE id = ?
     * when the drivers table migration is complete.
     */
    @Override
    public boolean isApproved(UUID driverId) {
        // Stub: will be replaced with actual driver qualification check
        return vehicleRepository.existsByDriverId(driverId);
    }

    /**
     * Returns true if the vehicle exists, belongs to the driver, and is APPROVED.
     */
    @Override
    public boolean hasOwnQualifiedVehicle(UUID driverId, UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .map(v -> v.getDriverId().equals(driverId) &&
                          "APPROVED".equalsIgnoreCase(v.getQualificationStatus()))
                .orElse(false);
    }
}
