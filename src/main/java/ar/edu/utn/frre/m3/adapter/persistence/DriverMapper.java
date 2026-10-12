package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.DriverStatus;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import ar.edu.utn.frre.m3.domain.QualificationInfo;
import org.springframework.stereotype.Component;

/**
 * Mapper bidireccional entre la entidad de dominio pura Driver y la entidad JPA DriverJpaEntity.
 */
@Component
public class DriverMapper {

    public Driver toDomain(DriverJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        String full=entity.getDisplayName().trim();
        String first=entity.getFirstName()!=null && !entity.getFirstName().isBlank() ? entity.getFirstName() : full;
        String last=entity.getFirstName()!=null && !entity.getFirstName().isBlank() ? entity.getLastName() : "";
        PersonalInfo personalInfo=PersonalInfo.fromStored(first,last,entity.getDni(),entity.getBirthDate(),
                entity.getAddress(),entity.getPhone(),entity.getEmail());

        DriverStatus status;
        try {
            status = entity.getQualificationStatus() != null
                    ? DriverStatus.valueOf(entity.getQualificationStatus())
                    : DriverStatus.PENDING;
        } catch (IllegalArgumentException e) {
            status = DriverStatus.PENDING;
        }

        QualificationInfo qualification = new QualificationInfo(status, entity.getQualificationReason());

        return new Driver(
                entity.getId(),
                entity.getIdentitySubject(),
                personalInfo,
                qualification,
                entity.getVersion() != null ? entity.getVersion().intValue() : 1,
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
        entity.setDisplayName(domain.getDisplayName());
        entity.setQualificationStatus(domain.getQualificationStatus() != null
                ? domain.getQualificationStatus().name()
                : "PENDING");
        entity.setQualificationReason(domain.getQualificationReason());
        entity.setVersion((long) domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        PersonalInfo info = domain.getPersonalInfo();
        if (info != null) {
            entity.setFirstName(info.getFirstName());
            entity.setLastName(info.getLastName());
            entity.setDni(info.getDni());
            entity.setBirthDate(info.getBirthDate());
            entity.setAddress(info.getAddress());
            entity.setPhone(info.getPhone());
            entity.setEmail(info.getEmail());
        }

        return entity;
    }
}
