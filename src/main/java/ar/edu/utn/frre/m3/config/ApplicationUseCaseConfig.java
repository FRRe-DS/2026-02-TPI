package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.port.AuditLogRepository;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.IdempotencyRepository;
import ar.edu.utn.frre.m3.application.port.OutboxRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.application.usecase.GetDriverVehiclesService;
import ar.edu.utn.frre.m3.application.usecase.GetDriverVehiclesUseCase;
import ar.edu.utn.frre.m3.application.usecase.RegisterDriverLicenseService;
import ar.edu.utn.frre.m3.application.usecase.RegisterDriverLicenseUseCase;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentService;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentUseCase;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityService;
import ar.edu.utn.frre.m3.application.usecase.RequestAvailabilityUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration providing Application Use Case beans for M3.
 * Follows Hexagonal Architecture: use cases are wired manually as plain Java
 * beans.
 */
@Configuration
public class ApplicationUseCaseConfig {

    @Bean
    public ar.edu.utn.frre.m3.application.usecase.DriverWorkspaceService driverWorkspaceService(
            DriverRepository drivers, VehicleRepository vehicles, DocumentRepository documents,
            AvailabilityIntentRepository availability,
            ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort principal, UnitOfWork transactions, Clock clock) {
        return new ar.edu.utn.frre.m3.application.usecase.DriverWorkspaceService(drivers, vehicles, documents, availability, principal, transactions, clock);
    }

    // ─── Shared infrastructure ───────────────────────────────────────────────

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    // ─── HU-01 / HU-02: Driver License ───────────────────────────────────────

    @Bean
    public RegisterDriverLicenseUseCase registerDriverLicenseUseCase(
            DriverRepository driverRepository, DocumentRepository documentRepository,
            UnitOfWork unitOfWork, IdempotencyRepository idempotencyRepository) {
        return new RegisterDriverLicenseService(driverRepository, documentRepository,
                unitOfWork, idempotencyRepository);
    }

    // ─── HU-06: Register Vehicle Document ────────────────────────────────────

    @Bean
    public RegisterVehicleDocumentUseCase registerVehicleDocumentUseCase(
            DocumentRepository documentRepository,
            VehicleRepository vehicleRepository,
            UnitOfWork unitOfWork) {
        return new RegisterVehicleDocumentService(
                documentRepository,
                vehicleRepository,
                unitOfWork);
    }

    // ─── HU-03: Vehicle Query / Vehicles ─────────────────────────────────────

    @Bean
    public GetDriverVehiclesUseCase getDriverVehiclesUseCase(VehicleRepository vehicleRepository) {
        return new GetDriverVehiclesService(vehicleRepository);
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
            Clock systemClock) {
        return new RequestAvailabilityService(
                driverRepository,
                availabilityIntentRepository,
                auditLogRepository,
                outboxRepository,
                idempotencyRepository,
                unitOfWork,
                systemClock);
    }
}
