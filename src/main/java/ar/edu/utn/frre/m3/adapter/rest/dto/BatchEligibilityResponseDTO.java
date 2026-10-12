package ar.edu.utn.frre.m3.adapter.rest.dto;

import java.util.List;
import java.util.UUID;

public record BatchEligibilityResponseDTO(
    List<DriverEligibilityEntry> results
) {
    public record DriverEligibilityEntry(
        UUID driverId,
        EligibilityResponseDTO eligibility
    ) {}
}
