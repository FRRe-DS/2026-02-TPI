package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;
import java.time.Instant;
import java.util.List;

public record EligibilityResponseDTO(
    boolean eligible,
    List<String> reasons,
    Instant checkedAt,
    int driverVersion,
    Instant validUntil
) {
    public static EligibilityResponseDTO from(EligibilityResult result) {
        return new EligibilityResponseDTO(
            result.eligible(),
            result.reasons(),
            result.checkedAt(),
            result.driverVersion(),
            result.validUntil()
        );
    }
}
