package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.Driver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistencia de conductores.
 * Permite a la capa de aplicación interactuar con el almacenamiento sin conocer JPA o la base de datos subyacente.
 */
public interface DriverRepositoryPort {

    Optional<Driver> findById(UUID id);

    Optional<Driver> findByIdentitySubject(String identitySubject);

    Driver save(Driver driver);

    boolean existsByIdentitySubject(String identitySubject);

    List<Driver> findAll();
}
