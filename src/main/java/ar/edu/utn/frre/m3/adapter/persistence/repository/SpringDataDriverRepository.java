package ar.edu.utn.frre.m3.adapter.persistence.repository;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverJpaEntity, UUID> {

    Optional<DriverJpaEntity> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DriverJpaEntity d WHERE d.id = :id")
    Optional<DriverJpaEntity> findByIdWithPessimisticLock(@Param("id") UUID id);
}
