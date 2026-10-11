package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.domain.Driver;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de persistencia que implementa el puerto DriverRepository.
 * Conecta el caso de uso con Spring Data JPA y aísla el modelo de dominio.
 */
@Repository
public class JpaDriverRepository implements DriverRepository {

    private final SpringDataDriverRepository springDataDriverRepository;

    public JpaDriverRepository(SpringDataDriverRepository springDataDriverRepository) {
        this.springDataDriverRepository = springDataDriverRepository;
    }

    @Override
    public Optional<Driver> findById(UUID id) {
        return springDataDriverRepository.findById(id)
                .map(DriverEntity::toDomain);
    }

    @Override
    public Optional<Driver> findByIdentitySubject(String identitySubject) {
        return springDataDriverRepository.findByIdentitySubject(identitySubject)
                .map(DriverEntity::toDomain);
    }

    @Override
    public boolean existsByIdentitySubject(String identitySubject) {
        return springDataDriverRepository.existsByIdentitySubject(identitySubject);
    }

    @Override
    public Driver save(Driver driver) {
        DriverEntity entity = DriverEntity.fromDomain(driver);
        DriverEntity savedEntity = springDataDriverRepository.save(entity);
        return savedEntity.toDomain();
    }
}
