package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAlreadyExistsException;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateDriverProfileUseCaseTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private CreateDriverProfileUseCase createDriverProfileUseCase;

    private final String identitySubject = "auth0|6600abcd1234";
    private final String driverName = "Carlos Tevez";

    @Test
    @DisplayName("Debe crear un nuevo perfil de conductor exitosamente con estado PENDING y versión 1")
    void shouldCreateDriverProfileSuccessfully() {
        when(driverRepository.existsByIdentitySubject(identitySubject)).thenReturn(false);
        when(driverRepository.findByIdentitySubject(identitySubject)).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Driver created = createDriverProfileUseCase.execute(identitySubject, driverName);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(identitySubject, created.getIdentitySubject());
        assertEquals(driverName, created.getName());
        assertEquals(QualificationStatus.PENDING, created.getQualificationStatus());
        assertEquals(1, created.getVersion());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    @DisplayName("Debe lanzar DriverAlreadyExistsException cuando el identity_subject ya existe")
    void shouldThrowExceptionWhenIdentitySubjectAlreadyExists() {
        when(driverRepository.existsByIdentitySubject(identitySubject)).thenReturn(true);

        DriverAlreadyExistsException exception = assertThrows(
                DriverAlreadyExistsException.class,
                () -> createDriverProfileUseCase.execute(identitySubject, driverName));

        assertEquals(identitySubject, exception.getIdentitySubject());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    @DisplayName("Debe rechazar nombres o identidades nulas/vacías")
    void shouldThrowExceptionWhenParametersAreInvalid() {
        assertThrows(IllegalArgumentException.class, () -> createDriverProfileUseCase.execute("", driverName));
        assertThrows(IllegalArgumentException.class, () -> createDriverProfileUseCase.execute(identitySubject, "   "));
    }
}
