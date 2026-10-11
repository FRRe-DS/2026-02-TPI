package ar.edu.utn.frre.m3.adapter.rest.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BatchEligibilityRequestDTO(
    @NotEmpty @Size(min=1, max=100) List<UUID> driverIds
) {}
