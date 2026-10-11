package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DriverPersistenceAdapter implements DriverRepository {

    private final SpringDataDriverRepository repository;

    public DriverPersistenceAdapter(SpringDataDriverRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Driver> findByIdWithPessimisticLock(UUID id) {
        return repository.findByIdWithPessimisticLock(id).map(this::toDomain);
    }

    @Override
    public Optional<Driver> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Driver> findByIdentitySubject(String identitySubject) {
        return repository.findByIdentitySubject(identitySubject).map(this::toDomain);
    }

    @Override
    public boolean existsByIdentitySubject(String identitySubject) {
        return repository.existsByIdentitySubject(identitySubject);
    }

    @Override
    public Driver save(Driver driver) {
        DriverJpaEntity entity = toEntity(driver);
        return toDomain(repository.save(entity));
    }

    private Driver toDomain(DriverJpaEntity entity) {
        return new Driver(
                entity.getId(),
                entity.getIdentitySubject(),
                entity.getDisplayName(),
                QualificationStatus.valueOf(entity.getQualificationStatus()),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DriverJpaEntity toEntity(Driver domain) {
        return new DriverJpaEntity(
                domain.getId(),
                domain.getIdentitySubject(),
                domain.getDisplayName(),
                domain.getQualificationStatus().name(),
                (long) domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
