package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.application.port.out.ClockPort;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverEligibilityPort;
import ar.edu.utn.frre.m3.domain.eligibility.DriverSnapshot;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityChecker;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;

import java.util.Objects;
import java.util.UUID;

public class CheckDriverAptitudeService implements CheckDriverAptitudeUseCase {

    private final DriverEligibilityPort driverEligibilityPort;
    private final EligibilityChecker eligibilityChecker;
    private final ClockPort clockPort;
    private final CurrentPrincipalPort currentPrincipal;

    public CheckDriverAptitudeService(DriverEligibilityPort driverEligibilityPort,
                                      EligibilityChecker eligibilityChecker,
                                      ClockPort clockPort,
                                      CurrentPrincipalPort currentPrincipal) {
        this.driverEligibilityPort = Objects.requireNonNull(driverEligibilityPort, "driverEligibilityPort es requerido");
        this.eligibilityChecker = Objects.requireNonNull(eligibilityChecker, "eligibilityChecker es requerido");
        this.clockPort = Objects.requireNonNull(clockPort, "clockPort es requerido");
        this.currentPrincipal = Objects.requireNonNull(currentPrincipal, "currentPrincipal es requerido");
    }

    @Override
    public EligibilityResult execute(UUID driverId) {
        Objects.requireNonNull(driverId, "driverId no puede ser nulo");
        DriverSnapshot snapshot = driverEligibilityPort.findSnapshotById(driverId)
                .orElseThrow(() -> DriverNotFoundException.forId(driverId));
        checkAuthorization(snapshot.identitySubject());
        return eligibilityChecker.check(snapshot, clockPort.now());
    }

    @Override
    public EligibilityResult executeForSelf() {
        if (!currentPrincipal.isAuthenticated()) {
            throw new DriverAccessDeniedException("Operación no autorizada: usuario no autenticado.");
        }
        String subject = currentPrincipal.getSubject()
                .orElseThrow(() -> new DriverAccessDeniedException("Identidad de usuario no disponible en el contexto."));
        DriverSnapshot snapshot = driverEligibilityPort.findSnapshotByIdentitySubject(subject)
                .orElseThrow(() -> DriverNotFoundException.forSubject(subject));
        return eligibilityChecker.check(snapshot, clockPort.now());
    }

    @Override
    public EligibilityResult executeBySubject(String identitySubject) {
        Objects.requireNonNull(identitySubject, "identitySubject no puede ser nulo");
        DriverSnapshot snapshot = driverEligibilityPort.findSnapshotByIdentitySubject(identitySubject)
                .orElseThrow(() -> DriverNotFoundException.forSubject(identitySubject));
        checkAuthorization(snapshot.identitySubject());
        return eligibilityChecker.check(snapshot, clockPort.now());
    }

    private void checkAuthorization(String targetSubject) {
        if (currentPrincipal.hasRole("SCOPE_m3:eligibility:read") || currentPrincipal.hasRole("SCOPE_m3:drivers:read") || currentPrincipal.hasRole("ROLE_OPERATOR") || currentPrincipal.hasRole("OPERATOR")
                || currentPrincipal.hasRole("ROLE_OPERADOR") || currentPrincipal.hasRole("OPERADOR")
                || currentPrincipal.hasRole("ROLE_ADMIN") || currentPrincipal.hasRole("ADMIN")) {
            return;
        }
        if (currentPrincipal.getSubject().filter(s -> s.equalsIgnoreCase(targetSubject)).isPresent()) {
            return;
        }
        throw new DriverAccessDeniedException("No tiene permisos para consultar la aptitud de este conductor.");
    }
}
