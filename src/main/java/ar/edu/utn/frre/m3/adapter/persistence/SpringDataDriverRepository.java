package ar.edu.utn.frre.m3.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA interno para operaciones CRUD directas en la base de datos de M3.
 * No es expuesto directamente fuera del adaptador de persistencia.
 */
@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverJpaEntity, UUID> {

    Optional<DriverJpaEntity> findByIdentitySubject(String identitySubject);

    boolean existsByIdentitySubject(String identitySubject);
}
