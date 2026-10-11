package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverRepositoryPort;
import ar.edu.utn.frre.m3.application.usecase.GetDriverProfileService;
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
}
