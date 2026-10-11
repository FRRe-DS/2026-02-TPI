package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.ServiceType;
import ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterDriverLicenseServiceTest {
    @Mock DriverRepository drivers;
    @Mock DocumentRepository documents;
    private RegisterDriverLicenseService service;
    private UUID driverId;
    private Driver driver;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        driver = new Driver(driverId, "subject", "Conductor", QualificationStatus.PENDING,
                1, Instant.now(), Instant.now());
        UnitOfWork unitOfWork = new UnitOfWork() {
            public <T> T executeInTransaction(Supplier<T> action) { return action.get(); }
            public void executeInTransaction(Runnable action) { action.run(); }
        };
        service = new RegisterDriverLicenseService(drivers, documents, unitOfWork);
    }

    private RegisterDriverLicenseCommand command(long version) {
        return new RegisterDriverLicenseCommand(driverId, "LIC-123",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
                Set.of(ServiceType.AUTO), null, version);
    }

    @Test
    void createsPendingLicenseAndIncrementsDriverVersion() {
        when(drivers.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documents.findCurrentLicenseByDriverId(driverId)).thenReturn(Optional.empty());
        when(documents.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Document created = service.register(command(1));

        assertEquals(1, created.getRevision());
        assertEquals(DocumentStatus.PENDING, created.getStatus());
        assertNull(created.getVehicleId());
        assertTrue(created.isCurrent());
        assertEquals(Set.of(ServiceType.AUTO), created.getServiceTypes());
        assertEquals(2, created.getDriverVersion());
        assertEquals(2, driver.getVersion());
    }

    @Test
    void renewalKeepsEarlierRevisionInHistory() {
        Document previous = Document.createPendingLicense(driverId, "OLD",
                Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                Set.of(ServiceType.MOTO), null, 1, 1);
        when(drivers.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        when(documents.findCurrentLicenseByDriverId(driverId)).thenReturn(Optional.of(previous));
        when(documents.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(drivers.findById(driverId)).thenReturn(Optional.of(driver));
        when(documents.findLicensesByDriverId(driverId)).thenReturn(List.of(previous));

        Document created = service.register(command(1));

        assertEquals(2, created.getRevision());
        assertFalse(previous.isCurrent());
        assertEquals(1, service.history(driverId).size());
        verify(documents, times(2)).save(any());
    }

    @Test
    void rejectsMissingDriverStaleVersionAndInvalidDates() {
        when(drivers.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.empty());
        assertThrows(DriverNotFoundException.class, () -> service.register(command(1)));

        when(drivers.findByIdWithPessimisticLock(driverId)).thenReturn(Optional.of(driver));
        assertThrows(VersionMismatchException.class, () -> service.register(command(2)));
        RegisterDriverLicenseCommand invalid = new RegisterDriverLicenseCommand(driverId, "LIC-123",
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                Set.of(ServiceType.AUTO), null, 1);
        assertThrows(InvalidDocumentPeriodException.class, () -> service.register(invalid));
        verify(documents, never()).save(any());
    }
}
