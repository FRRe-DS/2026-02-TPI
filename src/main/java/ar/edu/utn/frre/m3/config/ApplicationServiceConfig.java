package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.application.port.out.ClockPort;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverEligibilityPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.application.usecase.CheckDriverAptitudeService;
import ar.edu.utn.frre.m3.application.usecase.GetDriverProfileService;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityChecker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de inyección de dependencias para los casos de uso de Aplicación.
 * Mantiene la capa de aplicación desacoplada de Spring sin anotaciones @Service en los casos de uso.
 */
@Configuration
public class ApplicationServiceConfig {

    @Bean
    public GetDriverProfileService getDriverProfileService(DriverRepositoryPort driverRepositoryPort,
                                                           CurrentPrincipalPort currentPrincipalPort) {
        return new GetDriverProfileService(driverRepositoryPort, currentPrincipalPort);
    }

    @Bean
    public EligibilityChecker eligibilityChecker() {
        return new EligibilityChecker();
    }

    @Bean
    public CheckDriverAptitudeUseCase checkDriverAptitudeUseCase(
            DriverEligibilityPort driverEligibilityPort,
            EligibilityChecker eligibilityChecker,
            ClockPort clockPort,
            CurrentPrincipalPort currentPrincipalPort) {
        return new CheckDriverAptitudeService(driverEligibilityPort, eligibilityChecker, clockPort, currentPrincipalPort);
    }
}
