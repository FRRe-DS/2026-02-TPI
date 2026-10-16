package ar.edu.utn.frre.m3.adapter.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body of the M5 dispatch information query. */
public record DriverDispatchInfoRequest(
        @JsonProperty("conductor_id") @NotNull UUID conductorId) {
}
