package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.eligibility.DriverSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface DriverEligibilityPort {
    Optional<DriverSnapshot> findSnapshotById(UUID driverId);
    Optional<DriverSnapshot> findSnapshotByIdentitySubject(String identitySubject);
}
