package ar.edu.utn.frre.m3.adapter.rest.dto;

import java.util.List;

/**
 * Envoltorio para listas de vehículos según el esquema VehiclePage del contrato OpenAPI.
 */
public record VehiclePageResponse(
    List<VehicleResponse> items,
    String nextCursor
) {
}