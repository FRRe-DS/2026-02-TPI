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
import ar.edu.utn.frre.m3.application.usecase.GetDriverDispatchInfoService;
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
@org.springframework.scheduling.annotation.EnableScheduling
public class ApplicationUseCaseConfig {

    @Bean
    public ar.edu.utn.frre.m3.application.usecase.DriverWorkspaceService driverWorkspaceService(
            DriverRepository drivers, VehicleRepository vehicles, DocumentRepository documents,
            AvailabilityIntentRepository availability,
            ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort principal, UnitOfWork transactions, Clock clock) {
        return new ar.edu.utn.frre.m3.application.usecase.DriverWorkspaceService(drivers, vehicles, documents, availability, principal, transactions, clock);
    }

    @Bean
    public ar.edu.utn.frre.m3.application.usecase.DriverAccessService driverAccessService(DriverRepository drivers,
            ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort principal) {
        return new ar.edu.utn.frre.m3.application.usecase.DriverAccessService(drivers,principal);
    }
    @Bean
    public ar.edu.utn.frre.m3.application.usecase.QualificationService qualificationService(DriverRepository drivers,
            VehicleRepository vehicles, DocumentRepository documents, ar.edu.utn.frre.m3.application.port.QualificationRepository history,
            AuditLogRepository audit, UnitOfWork transactions, ar.edu.utn.frre.m3.application.usecase.DriverAccessService access, Clock clock) {
        return new ar.edu.utn.frre.m3.application.usecase.QualificationService(drivers,vehicles,documents,history,audit,transactions,access,clock);
    }
    @Bean
    public ar.edu.utn.frre.m3.application.usecase.CustomerRatingService customerRatingService(
            ar.edu.utn.frre.m3.application.usecase.DriverAccessService access, DriverRepository drivers,
            ar.edu.utn.frre.m3.application.port.CustomerRatingRepository ratings, ar.edu.utn.frre.m3.application.port.TripContextPort trips,
            UnitOfWork transactions,Clock clock) {
        return new ar.edu.utn.frre.m3.application.usecase.CustomerRatingService(access,drivers,ratings,trips,transactions,clock);
    }
    @Bean
    public ar.edu.utn.frre.m3.application.usecase.DeliverAvailabilityService deliverAvailabilityService(
            ar.edu.utn.frre.m3.application.port.AvailabilityDeliveryPort queue, ar.edu.utn.frre.m3.application.port.M4AvailabilityPort m4,Clock clock) {
        return new ar.edu.utn.frre.m3.application.usecase.DeliverAvailabilityService(queue,m4,clock);
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
            UnitOfWork unitOfWork, DriverRepository drivers) {
        return new RegisterVehicleDocumentService(
                documentRepository,
                vehicleRepository,
                unitOfWork, drivers);
    }

    // ─── HU-03: Vehicle Query / Vehicles ─────────────────────────────────────

    @Bean
    public GetDriverVehiclesUseCase getDriverVehiclesUseCase(VehicleRepository vehicleRepository) {
        return new GetDriverVehiclesService(vehicleRepository);
    }

    @Bean
    public ar.edu.utn.frre.m3.application.port.in.GetDriverDispatchInfoUseCase getDriverDispatchInfoUseCase(
            DriverRepository driverRepository,
            AvailabilityIntentRepository availabilityIntentRepository,
            VehicleRepository vehicleRepository) {
        return new GetDriverDispatchInfoService(
                driverRepository,
                availabilityIntentRepository,
                vehicleRepository);
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
            Clock systemClock, DocumentRepository documents, VehicleRepository vehicles) {
        return new RequestAvailabilityService(
                driverRepository,
                availabilityIntentRepository,
                auditLogRepository,
                outboxRepository,
                idempotencyRepository,
                unitOfWork,
                systemClock,documents,vehicles);
    }
}
