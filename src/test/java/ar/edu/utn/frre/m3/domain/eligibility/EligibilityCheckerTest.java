package ar.edu.utn.frre.m3.domain.eligibility;

import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.ServiceType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EligibilityCheckerTest {

    private final EligibilityChecker checker = new EligibilityChecker();
    private final Instant NOW = Instant.now();
    private final Instant FUTURE = NOW.plus(90, ChronoUnit.DAYS);
    private final Instant PAST = NOW.minus(1, ChronoUnit.DAYS);

    private DocumentSnapshot createLicense(QualificationStatus status, Instant validUntil) {
        return new DocumentSnapshot(UUID.randomUUID(), DocumentType.LICENSE, status, validUntil, NOW.minusSeconds(100), Set.of(ServiceType.AUTO));
    }

    private DocumentSnapshot createInsurance(QualificationStatus status, Instant validUntil) {
        return new DocumentSnapshot(UUID.randomUUID(), DocumentType.INSURANCE, status, validUntil, NOW.minusSeconds(100), Set.of(ServiceType.AUTO));
    }

    private VehicleSnapshot createVehicle(QualificationStatus status, DocumentSnapshot insurance) {
        return new VehicleSnapshot(UUID.randomUUID(), status, Set.of(ServiceType.AUTO), insurance, new DocumentSnapshot(UUID.randomUUID(),DocumentType.VEHICLE_REGISTRATION,QualificationStatus.APPROVED,FUTURE,NOW.minusSeconds(100),Set.of()));
    }

    private DriverSnapshot createDriver(QualificationStatus status, VehicleSnapshot vehicle, DocumentSnapshot license) {
        return new DriverSnapshot(UUID.randomUUID(), "subject", "John Doe", status, vehicle, license, 1);
    }

    private DriverSnapshot createValidDriver() {
        DocumentSnapshot insurance = createInsurance(QualificationStatus.APPROVED, FUTURE);
        VehicleSnapshot vehicle = createVehicle(QualificationStatus.APPROVED, insurance);
        DocumentSnapshot license = createLicense(QualificationStatus.APPROVED, FUTURE);
        return createDriver(QualificationStatus.APPROVED, vehicle, license);
    }

    @Nested
    class WhenDriverIsFullyEligible {
        @Test
        void givenAllConditionsMet_thenEligibleIsTrue() {
            DriverSnapshot driver = createValidDriver();
            EligibilityResult result = checker.check(driver, NOW);
            assertTrue(result.eligible());
        }

        @Test
        void givenAllConditionsMet_thenReasonsIsEmpty() {
            DriverSnapshot driver = createValidDriver();
            EligibilityResult result = checker.check(driver, NOW);
            assertTrue(result.reasons().isEmpty());
        }

        @Test
        void givenAllConditionsMet_thenValidUntilIsMinOfDocExpiries() {
            Instant closerFuture = NOW.plus(30, ChronoUnit.DAYS);
            DocumentSnapshot insurance = createInsurance(QualificationStatus.APPROVED, closerFuture);
            VehicleSnapshot vehicle = createVehicle(QualificationStatus.APPROVED, insurance);
            DocumentSnapshot license = createLicense(QualificationStatus.APPROVED, FUTURE);
            DriverSnapshot driver = createDriver(QualificationStatus.APPROVED, vehicle, license);

            EligibilityResult result = checker.check(driver, NOW);
            assertEquals(closerFuture, result.validUntil());
        }
    }

    @Nested
    class WhenDriverHasProblems {
        @Test
        void givenDriverIsPending_thenReasonIsDriverNotApproved() {
            DriverSnapshot driver = createValidDriver();
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), QualificationStatus.PENDING, driver.selectedVehicle(), driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.DRIVER_NOT_APPROVED));
        }

        @Test
        void givenDriverIsSuspended_thenReasonIsDriverSuspended() {
            DriverSnapshot driver = createValidDriver();
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), QualificationStatus.SUSPENDED, driver.selectedVehicle(), driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.DRIVER_SUSPENDED));
        }

        @Test
        void givenNoVehicleSelected_thenReasonIsNoVehicleSelected() {
            DriverSnapshot driver = createValidDriver();
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), null, driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.NO_VEHICLE_SELECTED));
        }

        @Test
        void givenVehicleNotApproved_thenReasonIsVehicleNotApproved() {
            DriverSnapshot driver = createValidDriver();
            VehicleSnapshot invalidVehicle = createVehicle(QualificationStatus.REJECTED, driver.selectedVehicle().insurance());
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), invalidVehicle, driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.VEHICLE_NOT_APPROVED));
        }

        @Test
        void givenLicenseMissing_thenReasonIsLicenseMissing() {
            DriverSnapshot driver = createValidDriver();
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), driver.selectedVehicle(), null, driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.LICENSE_MISSING));
        }

        @Test
        void givenLicenseExpired_thenReasonIsLicenseExpired() {
            DriverSnapshot driver = createValidDriver();
            DocumentSnapshot invalidLicense = createLicense(QualificationStatus.APPROVED, PAST);
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), driver.selectedVehicle(), invalidLicense, driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.LICENSE_EXPIRED));
        }

        @Test
        void givenInsuranceMissing_thenReasonIsInsuranceMissing() {
            DriverSnapshot driver = createValidDriver();
            VehicleSnapshot invalidVehicle = createVehicle(QualificationStatus.APPROVED, null);
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), invalidVehicle, driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.INSURANCE_MISSING));
        }

        @Test
        void givenInsuranceExpired_thenReasonIsInsuranceExpired() {
            DriverSnapshot driver = createValidDriver();
            DocumentSnapshot invalidInsurance = createInsurance(QualificationStatus.APPROVED, PAST);
            VehicleSnapshot invalidVehicle = createVehicle(QualificationStatus.APPROVED, invalidInsurance);
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), driver.qualificationStatus(), invalidVehicle, driver.license(), driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertTrue(result.reasons().contains(EligibilityReason.INSURANCE_EXPIRED));
        }
    }

    @Nested
    class WhenMultipleProblems {
        @Test
        void givenDriverSuspendedAndExpiredLicense_thenMultipleReasons() {
            DriverSnapshot driver = createValidDriver();
            DocumentSnapshot invalidLicense = createLicense(QualificationStatus.APPROVED, PAST);
            DriverSnapshot invalidDriver = new DriverSnapshot(driver.id(), driver.identitySubject(), driver.displayName(), QualificationStatus.SUSPENDED, driver.selectedVehicle(), invalidLicense, driver.version());
            EligibilityResult result = checker.check(invalidDriver, NOW);
            assertFalse(result.eligible());
            assertEquals(2, result.reasons().size());
            assertTrue(result.reasons().containsAll(List.of(EligibilityReason.DRIVER_SUSPENDED, EligibilityReason.LICENSE_EXPIRED)));
        }
    }
}
