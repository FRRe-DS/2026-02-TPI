package ar.edu.utn.frre.m3.adapter.rest.dto;

import ar.edu.utn.frre.m3.application.usecase.DriverDispatchInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** JSON representation of the M5 -> M3 dispatch information contract. */
public record DriverDispatchInfoResponse(
        @JsonProperty("conductor_id") UUID conductorId,
        @JsonProperty("habilitado") boolean habilitado,
        @JsonProperty("disponible") boolean disponible,
        @JsonProperty("vehiculo") @JsonInclude(JsonInclude.Include.ALWAYS) VehicleResponse vehiculo) {

    public static DriverDispatchInfoResponse from(DriverDispatchInfo dispatchInfo) {
        VehicleResponse vehicle = dispatchInfo.vehiculo() == null
                ? null
                : new VehicleResponse(
                        dispatchInfo.vehiculo().vehiculoId(),
                        dispatchInfo.vehiculo().tipoVehiculo());
        return new DriverDispatchInfoResponse(
                dispatchInfo.conductorId(),
                dispatchInfo.habilitado(),
                dispatchInfo.disponible(),
                vehicle);
    }

    public record VehicleResponse(
            @JsonProperty("vehiculo_id") UUID vehiculoId,
            @JsonProperty("tipo_vehiculo") String tipoVehiculo) {
    }
}
