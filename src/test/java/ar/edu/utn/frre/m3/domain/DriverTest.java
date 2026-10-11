package ar.edu.utn.frre.m3.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Dominio: Agregado Conductor (Driver)")
class DriverTest {

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
    @DisplayName("Debe inicializar un conductor en estado PENDING con versión 1")
    void shouldInitializeDriverInPendingStatusWithVersionOne() {
        UUID id = UUID.randomUUID();
        String identitySubject = "keycloak-sub-123";
        PersonalInfo info = createSamplePersonalInfo();
        Instant now = Instant.now();

        Driver driver = new Driver(id, identitySubject, info, now);

        assertEquals(id, driver.getId());
        assertEquals(identitySubject, driver.getIdentitySubject());
        assertEquals("Lautaro González", driver.getPersonalInfo().getFullName());
        assertEquals(DriverStatus.PENDING, driver.getStatus());
        assertFalse(driver.isOperable());
        assertEquals(1, driver.getVersion());
        assertEquals(now, driver.getCreatedAt());
        assertEquals(now, driver.getUpdatedAt());
    }

    @Test
    @DisplayName("Debe verificar correctamente la propiedad del perfil mediante identitySubject")
    void shouldVerifyOwnershipByIdentitySubject() {
        UUID id = UUID.randomUUID();
        String identitySubject = "keycloak-sub-123";
        Driver driver = new Driver(id, identitySubject, createSamplePersonalInfo(), Instant.now());

        assertTrue(driver.isOwnedBy("keycloak-sub-123"));
        assertFalse(driver.isOwnedBy("other-sub-456"));
        assertFalse(driver.isOwnedBy(null));
    }

    @Test
    @DisplayName("Debe incrementar la versión al actualizar el estado de habilitación")
    void shouldIncrementVersionWhenUpdatingQualification() {
        Driver driver = new Driver(UUID.randomUUID(), "sub-1", createSamplePersonalInfo(), Instant.now());
        assertEquals(1, driver.getVersion());

        driver.updateQualification(QualificationInfo.approved(), Instant.now());

        assertEquals(2, driver.getVersion());
        assertEquals(DriverStatus.APPROVED, driver.getStatus());
        assertTrue(driver.isOperable());
    }

    @Test
    @DisplayName("Debe rechazar la creación si el DNI o email son inválidos")
    void shouldRejectInvalidPersonalInfo() {
        assertThrows(IllegalArgumentException.class, () ->
                new PersonalInfo("", "González", "42158963", LocalDate.now(), "Dirección", "123", "correo@test.com"));

        assertThrows(IllegalArgumentException.class, () ->
                new PersonalInfo("Lautaro", "González", "42158963", LocalDate.now(), "Dirección", "123", "correo-invalido-sin-arroba"));
    }
}
