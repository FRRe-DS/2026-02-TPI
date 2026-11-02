package ar.edu.utn.frre.m3.adapter.rest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** M5 dispatch notification consumed by the driver's workspace. */
public record M5DriverOfferRequest(
        @JsonAlias("id_oferta") @NotNull UUID offerId,
        @JsonAlias("id_conductor") @NotNull UUID driverId,
        @JsonAlias("id_solicitud") @NotBlank @Size(max = 128) String requestId,
        @JsonAlias("origen") @NotBlank @Size(max = 240) String pickupAddress,
        @JsonAlias("destino") @NotBlank @Size(max = 240) String destinationAddress,
        @JsonAlias("tipo_vehiculo") @NotBlank @Size(max = 40) String vehicleType,
        @JsonAlias("vence_en") @NotNull OffsetDateTime expiresAt) {
}
