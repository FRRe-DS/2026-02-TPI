package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.application.port.out.ClockPort;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.port.out.DriverEligibilityPort;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.ServiceType;
import ar.edu.utn.frre.m3.domain.eligibility.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CheckDriverAptitudeUseCaseTest {

    private DriverEligibilityPort driverEligibilityPort;
    private ClockPort clockPort;
    private CurrentPrincipalPort currentPrincipal;
    private EligibilityChecker eligibilityChecker;
    private CheckDriverAptitudeUseCase useCase;

    private Instant now;

    @BeforeEach
    void setUp() {
        driverEligibilityPort = mock(DriverEligibilityPort.class);
        clockPort = mock(ClockPort.class);
        currentPrincipal = mock(CurrentPrincipalPort.class);
        eligibilityChecker = new EligibilityChecker();
        useCase = new CheckDriverAptitudeService(driverEligibilityPort, eligibilityChecker, clockPort, currentPrincipal);
        now = Instant.now();
        when(clockPort.now()).thenReturn(now);
    }

    private DriverSnapshot createEligibleSnapshot(UUID driverId, String subject) {
        DocumentSnapshot license = new DocumentSnapshot(UUID.randomUUID(), DocumentType.LICENSE, QualificationStatus.APPROVED, now.plus(10, ChronoUnit.DAYS));
        DocumentSnapshot insurance = new DocumentSnapshot(UUID.randomUUID(), DocumentType.INSURANCE, QualificationStatus.APPROVED, now.plus(10, ChronoUnit.DAYS));
        VehicleSnapshot vehicle = new VehicleSnapshot(UUID.randomUUID(), QualificationStatus.APPROVED, Set.of(ServiceType.AUTO), insurance);
        return new DriverSnapshot(driverId, subject, "Test Driver", QualificationStatus.APPROVED, vehicle, license, 1);
    }

    @Test
    void execute_withEligibleDriver_returnsEligibleResult() {
        UUID driverId = UUID.randomUUID();
        String subject = "test-subject";
        DriverSnapshot snapshot = createEligibleSnapshot(driverId, subject);

        when(driverEligibilityPort.findSnapshotById(driverId)).thenReturn(Optional.of(snapshot));
        when(currentPrincipal.hasRole("ROLE_OPERATOR")).thenReturn(true);

        EligibilityResult result = useCase.execute(driverId);

        assertTrue(result.eligible());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void execute_withDriverNotFound_throwsDriverNotFoundException() {
        UUID driverId = UUID.randomUUID();
        when(driverEligibilityPort.findSnapshotById(driverId)).thenReturn(Optional.empty());
        when(currentPrincipal.hasRole("ROLE_OPERATOR")).thenReturn(true);

        assertThrows(DriverNotFoundException.class, () -> useCase.execute(driverId));
    }

    @Test
    void executeForSelf_withEligibleDriver_returnsEligibleResult() {
        UUID driverId = UUID.randomUUID();
        String subject = "self-subject";
        DriverSnapshot snapshot = createEligibleSnapshot(driverId, subject);

        when(currentPrincipal.isAuthenticated()).thenReturn(true);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of(subject));
        when(driverEligibilityPort.findSnapshotByIdentitySubject(subject)).thenReturn(Optional.of(snapshot));

        EligibilityResult result = useCase.executeForSelf();

        assertTrue(result.eligible());
    }

    @Test
    void execute_withNonOwnerNonOperator_throwsAccessDeniedException() {
        UUID driverId = UUID.randomUUID();
        String subject = "owner-subject";
        DriverSnapshot snapshot = createEligibleSnapshot(driverId, subject);

        when(driverEligibilityPort.findSnapshotById(driverId)).thenReturn(Optional.of(snapshot));
        when(currentPrincipal.hasRole(anyString())).thenReturn(false);
        when(currentPrincipal.getSubject()).thenReturn(Optional.of("other-subject"));

        assertThrows(DriverAccessDeniedException.class, () -> useCase.execute(driverId));
    }
}
