package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de repositorio para el agregado Driver según la arquitectura hexagonal
 * de M3.
 */
public interface DriverRepository {

    Optional<Driver> findById(UUID id);

    Optional<Driver> findByIdWithPessimisticLock(UUID id);

    Optional<Driver> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);

    Driver save(Driver driver);

    default List<Driver> findAll() {
        return List.of();
    }

    /**
     * Returns true if the driver exists in M3.
     */
    boolean existsById(UUID driverId);

    /**
     * Returns true if the driver has APPROVED qualification status.
     */
    boolean isApproved(UUID driverId);

    /**
     * Returns true if the given vehicle exists, is APPROVED, and belongs to the
     * driver.
     */
    boolean hasOwnQualifiedVehicle(UUID driverId, UUID vehicleId);
}
