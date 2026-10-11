package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Driver;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de repositorio para la entidad Driver según sección 3.5.
 */
public interface DriverRepository {

    Optional<Driver> findById(UUID id);

    Optional<Driver> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);

    Driver save(Driver driver);
}
