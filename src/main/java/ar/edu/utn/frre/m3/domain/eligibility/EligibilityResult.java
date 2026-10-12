package ar.edu.utn.frre.m3.domain.eligibility;

import java.time.Instant;
import java.util.List;

public record EligibilityResult(
        boolean eligible,
        List<String> reasons,
        Instant checkedAt,
        int driverVersion,
        Instant validUntil
) {
}
