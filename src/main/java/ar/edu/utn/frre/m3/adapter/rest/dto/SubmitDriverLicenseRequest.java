package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record SubmitDriverLicenseRequest(
        @NotBlank @Size(max = 80) String documentNumber,
        @NotNull Instant issuedAt,
        @NotNull Instant validUntil,
        @NotEmpty Set<@NotNull ServiceType> serviceTypes,
        @Size(max = 200) String storageReference
) {}
