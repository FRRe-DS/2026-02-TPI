package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.OutboxRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentService;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentUseCase;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityService;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring application use cases and their port dependencies.
 * Keeps domain and application use case classes decoupled from Spring annotations.
 */
@Configuration
public class ApplicationUseCaseConfig {

    // ─── Shared infrastructure ───────────────────────────────────────────────

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    // ─── HU-06: Register Vehicle Document ────────────────────────────────────

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

    // ─── HU-10: Request Availability ─────────────────────────────────────────

    @Bean
    public RequestAvailabilityUseCase requestAvailabilityUseCase(
        DriverRepository driverRepository,
        AvailabilityIntentRepository availabilityIntentRepository,
        AuditLogRepository auditLogRepository,
        OutboxRepository outboxRepository,
        IdempotencyRepository idempotencyRepository,
        UnitOfWork unitOfWork,
        Clock systemClock
    ) {
        return new RequestAvailabilityService(
            driverRepository,
            availabilityIntentRepository,
            auditLogRepository,
            outboxRepository,
            idempotencyRepository,
            unitOfWork,
            systemClock
        );
    }
}
