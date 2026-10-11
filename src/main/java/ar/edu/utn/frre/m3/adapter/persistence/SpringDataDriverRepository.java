package ar.edu.utn.frre.m3.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio interno de Spring Data JPA para la entidad DriverEntity.
 */
@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverEntity, UUID> {

    Optional<DriverEntity> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);
}
