package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.DriverStatus;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import ar.edu.utn.frre.m3.domain.QualificationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({DriverPersistenceAdapter.class, DriverMapper.class})
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de Persistencia: DriverPersistenceAdapter")
class DriverPersistenceAdapterTest {

    @Autowired
    private DriverPersistenceAdapter persistenceAdapter;

    private PersonalInfo createSamplePersonalInfo() {
        return new PersonalInfo(
                "Lautaro",
                "González",
                "42158963",
                LocalDate.of(1999, 8, 20),
                "Av. Castelli 950",
                "+54 362 4558899",
                "lautaro.gonzalez@example.com"
        );
    }

    @Test
    @DisplayName("Debe persistir y recuperar un conductor por ID")
    void shouldSaveAndFindDriverById() {
        UUID id = UUID.randomUUID();
        String identitySubject = "sub-test-123";
        Driver driver = new Driver(id, identitySubject, createSamplePersonalInfo(), Instant.now());

        persistenceAdapter.save(driver);

        Optional<Driver> found = persistenceAdapter.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(identitySubject, found.get().getIdentitySubject());
        assertEquals("Lautaro González", found.get().getPersonalInfo().getFullName());
        assertEquals(DriverStatus.PENDING, found.get().getStatus());
        assertEquals(1, found.get().getVersion());
    }

    @Test
    @DisplayName("Debe recuperar un conductor por identitySubject")
    void shouldFindDriverByIdentitySubject() {
        UUID id = UUID.randomUUID();
        String identitySubject = "keycloak-user-789";
        Driver driver = new Driver(
                id,
                identitySubject,
                createSamplePersonalInfo(),
                QualificationInfo.approved(),
                1,
                Instant.now(),
                Instant.now()
        );

        persistenceAdapter.save(driver);

        Optional<Driver> found = persistenceAdapter.findByIdentitySubject(identitySubject);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals(DriverStatus.APPROVED, found.get().getStatus());
        assertTrue(found.get().isOperable());
    }

    @Test
    @DisplayName("existsByIdentitySubject debe retornar true si existe y false si no existe")
    void shouldCheckExistenceByIdentitySubject() {
        String existingSub = "sub-existing-456";
        Driver driver = new Driver(UUID.randomUUID(), existingSub, createSamplePersonalInfo(), Instant.now());
        persistenceAdapter.save(driver);

        assertTrue(persistenceAdapter.existsByIdentitySubject(existingSub));
        assertFalse(persistenceAdapter.existsByIdentitySubject("non-existent-sub"));
    }
}
