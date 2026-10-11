package ar.edu.utn.frre.m3.application.port;

import java.util.UUID;

/**
 * Output port for driver existence and qualification checks.
 *
 * <p>M3 needs to verify:
 * <ol>
 *   <li>Driver exists (otherwise 404).</li>
 *   <li>Driver is APPROVED and has a qualified own vehicle (otherwise 422).</li>
 * </ol>
 */
public interface DriverRepository {

    /**
     * Returns true if the driver exists in M3.
     */
    boolean existsById(UUID driverId);

    /**
     * Returns true if the driver has APPROVED qualification status.
     */
    boolean isApproved(UUID driverId);

    /**
     * Returns true if the given vehicle exists, is APPROVED, and belongs to the driver.
     */
    boolean hasOwnQualifiedVehicle(UUID driverId, UUID vehicleId);
}
