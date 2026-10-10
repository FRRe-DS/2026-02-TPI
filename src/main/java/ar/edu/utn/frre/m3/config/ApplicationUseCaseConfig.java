package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentService;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring application use cases and their port dependencies.
 * Keeps domain and application use case classes decoupled from Spring annotations.
 */
@Configuration
public class ApplicationUseCaseConfig {

    @Bean
    public RegisterVehicleDocumentUseCase registerVehicleDocumentUseCase(
        DocumentRepository documentRepository,
        VehicleRepository vehicleRepository,
        UnitOfWork unitOfWork
    ) {
        return new RegisterVehicleDocumentService(
            documentRepository,
            vehicleRepository,
            unitOfWork
        );
    }
}

