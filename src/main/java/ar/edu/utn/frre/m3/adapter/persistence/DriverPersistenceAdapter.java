package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.domain.Driver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia que implementa el puerto DriverRepositoryPort.
 * Encapsula el acceso a datos mediante Spring Data JPA y garantiza el aislamiento de la lógica de dominio.
 */
@Component
public class DriverPersistenceAdapter implements DriverRepositoryPort {

    private final SpringDataDriverRepository springDataRepository;
    private final DriverMapper driverMapper;

    public DriverPersistenceAdapter(SpringDataDriverRepository springDataRepository,
                                  DriverMapper driverMapper) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository es requerido");
        this.driverMapper = Objects.requireNonNull(driverMapper, "driverMapper es requerido");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Driver> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(driverMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Driver> findByIdentitySubject(String identitySubject) {
        return springDataRepository.findByIdentitySubject(identitySubject)
                .map(driverMapper::toDomain);
    }

    @Override
    @Transactional
    public Driver save(Driver driver) {
        DriverJpaEntity entity = driverMapper.toJpaEntity(driver);
        DriverJpaEntity savedEntity = springDataRepository.save(entity);
        return driverMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdentitySubject(String identitySubject) {
        return springDataRepository.existsByIdentitySubject(identitySubject);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> findAll() {
        return springDataRepository.findAll().stream()
                .map(driverMapper::toDomain)
                .collect(Collectors.toList());
    }
}
