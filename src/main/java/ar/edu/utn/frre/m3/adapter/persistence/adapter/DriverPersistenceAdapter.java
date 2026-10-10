package ar.edu.utn.frre.m3.adapter.persistence.adapter;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.domain.model.Driver;
import ar.edu.utn.frre.m3.domain.model.QualificationStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DriverPersistenceAdapter implements DriverRepositoryPort {

    private final SpringDataDriverRepository driverRepository;

    public DriverPersistenceAdapter(SpringDataDriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    public Optional<Driver> findByIdWithPessimisticLock(UUID driverId) {
        return driverRepository.findByIdWithPessimisticLock(driverId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Driver> findById(UUID driverId) {
        return driverRepository.findById(driverId)
                .map(this::toDomain);
    }

    @Override
    public Driver save(Driver driver) {
        DriverJpaEntity entity = toEntity(driver);
        DriverJpaEntity saved = driverRepository.save(entity);
        return toDomain(saved);
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
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
