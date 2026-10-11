package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de Aplicación: GetDriverProfileService (HU-02)")
class GetDriverProfileServiceTest {

    @Mock
    private DriverRepositoryPort driverRepository;

    @Mock
    private CurrentPrincipalPort currentPrincipal;

    private GetDriverProfileService service;

    private Driver sampleDriver;
    private final UUID driverId = UUID.randomUUID();
    private final String ownerSubject = "user-subject-123";

    @BeforeEach
    void setUp() {
        service = new GetDriverProfileService(driverRepository, currentPrincipal);

        PersonalInfo info = new PersonalInfo(
                "Lautaro",
                "González",
                "42158963",
                LocalDate.of(1999, 8, 20),
                "Av. Castelli 950",
                "+54 362 4558899",
                "lautaro.gonzalez@example.com"
        );
        sampleDriver = new Driver(driverId, ownerSubject, info, Instant.now());
    }

    @Test
    @DisplayName("getMyDriverProfile debe retornar el perfil del conductor autenticado")
    void shouldReturnProfileForAuthenticatedDriver() {
        when(currentPrincipal.isAuthenticated()).thenReturn(true);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of(ownerSubject));
        when(driverRepository.findByIdentitySubject(ownerSubject)).thenReturn(Optional.of(sampleDriver));

        Driver result = service.getMyDriverProfile();

        assertNotNull(result);
        assertEquals(driverId, result.getId());
        assertEquals(ownerSubject, result.getIdentitySubject());
        verify(driverRepository).findByIdentitySubject(ownerSubject);
    }

    @Test
    @DisplayName("getMyDriverProfile debe lanzar excepción si la sesión no está autenticada")
    void shouldThrowExceptionWhenUnauthenticated() {
        when(currentPrincipal.isAuthenticated()).thenReturn(false);

        assertThrows(DriverAccessDeniedException.class, () -> service.getMyDriverProfile());
        verifyNoInteractions(driverRepository);
    }

    @Test
    @DisplayName("getMyDriverProfile debe lanzar DriverNotFoundException si el perfil no existe")
    void shouldThrowDriverNotFoundWhenDriverDoesNotExist() {
        when(currentPrincipal.isAuthenticated()).thenReturn(true);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of(ownerSubject));
        when(driverRepository.findByIdentitySubject(ownerSubject)).thenReturn(Optional.empty());

        assertThrows(DriverNotFoundException.class, () -> service.getMyDriverProfile());
    }

    @Test
    @DisplayName("getDriverProfileById debe permitir acceso al propietario del perfil")
    void shouldAllowAccessToOwner() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(sampleDriver));
        when(currentPrincipal.hasRole("ROLE_OPERATOR")).thenReturn(false);
        when(currentPrincipal.hasRole("OPERATOR")).thenReturn(false);
        when(currentPrincipal.hasRole("ROLE_ADMIN")).thenReturn(false);
        when(currentPrincipal.hasRole("ADMIN")).thenReturn(false);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of(ownerSubject));

        Driver result = service.getDriverProfileById(driverId);

        assertNotNull(result);
        assertEquals(driverId, result.getId());
    }

    @Test
    @DisplayName("getDriverProfileById debe permitir acceso a un operador aunque no sea propietario")
    void shouldAllowAccessToOperator() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(sampleDriver));
        when(currentPrincipal.hasRole("ROLE_OPERATOR")).thenReturn(true);

        Driver result = service.getDriverProfileById(driverId);

        assertNotNull(result);
        assertEquals(driverId, result.getId());
    }

    @Test
    @DisplayName("getDriverProfileById debe denegar acceso a otro usuario que no es operador")
    void shouldDenyAccessToUnauthorizedUser() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(sampleDriver));
        when(currentPrincipal.hasRole("ROLE_OPERATOR")).thenReturn(false);
        when(currentPrincipal.hasRole("OPERATOR")).thenReturn(false);
        when(currentPrincipal.hasRole("ROLE_ADMIN")).thenReturn(false);
        when(currentPrincipal.hasRole("ADMIN")).thenReturn(false);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of("other-user-999"));

        assertThrows(DriverAccessDeniedException.class, () -> service.getDriverProfileById(driverId));
    }
}
