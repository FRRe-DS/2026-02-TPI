package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAlreadyExistsException;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.domain.Driver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Crear perfil de conductor (HU-01 / CLON-6).
 * Valida que no existan perfiles duplicados para el mismo identity_subject
 * antes de persistir el nuevo conductor.
 */
@Service
@Transactional
public class CreateDriverProfileUseCase {

    private final DriverRepository driverRepository;

    public CreateDriverProfileUseCase(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public Driver execute(String identitySubject, String name) {
        if (identitySubject == null || identitySubject.trim().isEmpty()) {
            throw new IllegalArgumentException("identitySubject no puede ser nulo ni vacío");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name no puede ser nulo ni vacío");
        }

        String normalizedIdentity = identitySubject.trim();

        if (driverRepository.existsByIdentitySubject(normalizedIdentity)
                || driverRepository.findByIdentitySubject(normalizedIdentity).isPresent()) {
            throw new DriverAlreadyExistsException(normalizedIdentity);
        }

        Driver newDriver = new Driver(normalizedIdentity, name.trim());
        return driverRepository.save(newDriver);
    }

    public Driver execute(CreateDriverCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando no puede ser nulo");
        }
        return execute(command.identitySubject(), command.name());
    }

    public record CreateDriverCommand(String identitySubject, String name) {}
}
