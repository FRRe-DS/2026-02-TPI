package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.domain.Driver;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository {
    Optional<Driver> findByIdWithPessimisticLock(UUID id);
    Optional<Driver> findById(UUID id);
    Driver save(Driver driver);
}
