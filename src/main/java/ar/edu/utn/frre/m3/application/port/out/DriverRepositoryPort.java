package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.model.Driver;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepositoryPort {
    Optional<Driver> findByIdWithPessimisticLock(UUID driverId);
    Optional<Driver> findById(UUID driverId);
    Driver save(Driver driver);
}
