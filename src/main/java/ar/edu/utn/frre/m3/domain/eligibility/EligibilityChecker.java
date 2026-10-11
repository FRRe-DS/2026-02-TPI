package ar.edu.utn.frre.m3.domain.eligibility;

import ar.edu.utn.frre.m3.domain.QualificationStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class EligibilityChecker {

    public EligibilityResult check(DriverSnapshot driver, Instant now) {
        List<String> reasons = new ArrayList<>();
        Instant validUntil = now.plus(365, ChronoUnit.DAYS);

        // Check driver qualification
        if (driver.qualificationStatus() == QualificationStatus.SUSPENDED) {
            reasons.add(EligibilityReason.DRIVER_SUSPENDED);
        } else if (!driver.qualificationStatus().isApproved()) {
            reasons.add(EligibilityReason.DRIVER_NOT_APPROVED);
        }

        // Check selected vehicle
        VehicleSnapshot vehicle = driver.selectedVehicle();
        if (vehicle == null) {
            reasons.add(EligibilityReason.NO_VEHICLE_SELECTED);
        } else {
            if (vehicle.qualificationStatus() == QualificationStatus.SUSPENDED) {
                reasons.add(EligibilityReason.VEHICLE_SUSPENDED);
            } else if (!vehicle.qualificationStatus().isApproved()) {
                reasons.add(EligibilityReason.VEHICLE_NOT_APPROVED);
            }
        }

        // Check license
        DocumentSnapshot license = driver.license();
        if (license == null) {
            reasons.add(EligibilityReason.LICENSE_MISSING);
        } else if (!license.qualificationStatus().isApproved()) {
            reasons.add(EligibilityReason.LICENSE_NOT_APPROVED);
        } else if (license.isExpiredAt(now)) {
            reasons.add(EligibilityReason.LICENSE_EXPIRED);
        } else {
            if (license.validUntil() != null) {
                validUntil = minInstant(validUntil, license.validUntil());
            }
        }

        // Check insurance
        if (vehicle != null) {
            DocumentSnapshot insurance = vehicle.insurance();
            if (insurance == null) {
                reasons.add(EligibilityReason.INSURANCE_MISSING);
            } else if (!insurance.qualificationStatus().isApproved()) {
                reasons.add(EligibilityReason.INSURANCE_NOT_APPROVED);
            } else if (insurance.isExpiredAt(now)) {
                reasons.add(EligibilityReason.INSURANCE_EXPIRED);
            } else {
                if (insurance.validUntil() != null) {
                    validUntil = minInstant(validUntil, insurance.validUntil());
                }
            }
        }

        return new EligibilityResult(
                reasons.isEmpty(),
                List.copyOf(reasons),
                now,
                driver.version(),
                validUntil
        );
    }

    private Instant minInstant(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }
}
