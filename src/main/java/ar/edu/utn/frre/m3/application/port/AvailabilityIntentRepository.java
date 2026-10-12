package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port: persistence for {@link AvailabilityIntent}.
 * Implemented by the persistence adapter (JPA).
 */
public interface AvailabilityIntentRepository {

    /**
     * Finds the current intent for the given driver, if any.
     */
    Optional<AvailabilityIntent> findByDriverId(UUID driverId);

    /**
     * Persists a new or updated availability intent.
     *
     * @return The saved intent (may have DB-generated fields).
     */
    AvailabilityIntent save(AvailabilityIntent intent);
}
