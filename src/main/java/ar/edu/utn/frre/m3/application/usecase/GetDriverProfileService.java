package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.domain.Driver;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de Aplicación que implementa los casos de uso de consulta de perfil de conductor (HU-02).
 * Aplica rigurosamente las reglas de seguridad sobre objetos:
 * - El conductor solo puede ver su propio perfil.
 * - Los operadores administrativos pueden consultar cualquier perfil.
 */
public class GetDriverProfileService implements GetDriverProfileUseCase, GetMyDriverProfileUseCase {

    private final DriverRepositoryPort driverRepository;
    private final CurrentPrincipalPort currentPrincipal;

    public GetDriverProfileService(DriverRepositoryPort driverRepository,
                                  CurrentPrincipalPort currentPrincipal) {
        this.driverRepository = Objects.requireNonNull(driverRepository, "driverRepository es requerido");
        this.currentPrincipal = Objects.requireNonNull(currentPrincipal, "currentPrincipal es requerido");
    }

    @Override
    public Driver getMyDriverProfile() {
        if (!currentPrincipal.isAuthenticated()) {
            throw new DriverAccessDeniedException("Operación no autorizada: sesión o token no autenticado.");
        }

        String subject = currentPrincipal.getSubject()
                .orElseThrow(() -> new DriverAccessDeniedException("Identidad de usuario (subject) no disponible en el contexto de seguridad."));

        return driverRepository.findByIdentitySubject(subject)
                .orElseThrow(() -> DriverNotFoundException.forSubject(subject));
    }

    @Override
    public Driver getDriverProfileById(UUID driverId) {
        Objects.requireNonNull(driverId, "El driverId no puede ser nulo");

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> DriverNotFoundException.forId(driverId));

        if (!hasAccessToDriver(driver)) {
            throw new DriverAccessDeniedException("Acceso denegado: no posee permisos para consultar la información de este conductor.");
        }

        return driver;
    }

    private boolean hasAccessToDriver(Driver driver) {
        // 1. Si es operador o administrador del sistema, tiene acceso irrestricto de consulta
        if (currentPrincipal.hasRole("ROLE_OPERATOR") || currentPrincipal.hasRole("OPERATOR")
                || currentPrincipal.hasRole("ROLE_ADMIN") || currentPrincipal.hasRole("ADMIN")) {
            return true;
        }

        // 2. Si es el propietario del perfil (mismo subject de M1)
        return currentPrincipal.getSubject()
                .map(driver::isOwnedBy)
                .orElse(false);
    }
}
