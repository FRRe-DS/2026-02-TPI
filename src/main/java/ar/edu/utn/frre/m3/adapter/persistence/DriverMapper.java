package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.DriverStatus;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import ar.edu.utn.frre.m3.domain.QualificationInfo;
import org.springframework.stereotype.Component;

/**
 * Mapper bidireccional entre la entidad de dominio pura Driver y la entidad JPA de persistencia DriverJpaEntity.
 */
@Component
public class DriverMapper {

    public Driver toDomain(DriverJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        PersonalInfo personalInfo = new PersonalInfo(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDni(),
                entity.getBirthDate(),
                entity.getAddress(),
                entity.getPhone(),
                entity.getEmail()
        );

        DriverStatus status = DriverStatus.valueOf(entity.getStatus());
        QualificationInfo qualification = new QualificationInfo(status, entity.getQualificationReason());

        return new Driver(
                entity.getId(),
                entity.getIdentitySubject(),
                personalInfo,
                qualification,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DriverJpaEntity toJpaEntity(Driver domain) {
        if (domain == null) {
            return null;
        }

        DriverJpaEntity entity = new DriverJpaEntity();
        entity.setId(domain.getId());
        entity.setIdentitySubject(domain.getIdentitySubject());
        entity.setFirstName(domain.getPersonalInfo().getFirstName());
        entity.setLastName(domain.getPersonalInfo().getLastName());
        entity.setDni(domain.getPersonalInfo().getDni());
        entity.setBirthDate(domain.getPersonalInfo().getBirthDate());
        entity.setAddress(domain.getPersonalInfo().getAddress());
        entity.setPhone(domain.getPersonalInfo().getPhone());
        entity.setEmail(domain.getPersonalInfo().getEmail());
        entity.setStatus(domain.getStatus().name());
        entity.setQualificationReason(domain.getQualificationReason());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }
}
