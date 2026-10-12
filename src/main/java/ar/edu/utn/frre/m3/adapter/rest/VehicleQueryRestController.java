package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.VehiclePageResponse;
import ar.edu.utn.frre.m3.adapter.rest.dto.VehicleResponse;
import ar.edu.utn.frre.m3.application.usecase.GetDriverVehiclesUseCase;
import ar.edu.utn.frre.m3.domain.Vehicle;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consultar vehículos del conductor (HU-04 / RF-3.2).
 */
@RestController
@RequestMapping("/api/v1/drivers/{driverId}/vehicles")
public class VehicleQueryRestController {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private final GetDriverVehiclesUseCase getDriverVehiclesUseCase;

    public VehicleQueryRestController(GetDriverVehiclesUseCase getDriverVehiclesUseCase) {
        this.getDriverVehiclesUseCase = Objects.requireNonNull(getDriverVehiclesUseCase, "getDriverVehiclesUseCase cannot be null");
    }

    /**
     * Lista todos los vehículos de un conductor.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VehiclePageResponse> listVehicles(
        @PathVariable UUID driverId,
        HttpServletRequest request
    ) {
        List<Vehicle> domainVehicles = getDriverVehiclesUseCase.listVehiclesByDriver(driverId);
        List<VehicleResponse> items = domainVehicles.stream()
            .map(VehicleResponse::fromDomain)
            .toList();

        String correlationId = resolveCorrelationId(request);

        return ResponseEntity.ok()
            .header(CORRELATION_HEADER, correlationId)
            .body(new VehiclePageResponse(items, null));
    }

    /**
     * Consulta el detalle de un vehículo puntual de un conductor.
     */
    @GetMapping(path = "/{vehicleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VehicleResponse> getVehicle(
        @PathVariable UUID driverId,
        @PathVariable UUID vehicleId,
        HttpServletRequest request
    ) {
        Vehicle vehicle = getDriverVehiclesUseCase.getVehicle(driverId, vehicleId);
        String correlationId = resolveCorrelationId(request);

        return ResponseEntity.ok()
            .header(CORRELATION_HEADER, correlationId)
            .header("ETag", "\"" + vehicle.getDriverVersion() + "\"")
            .body(VehicleResponse.fromDomain(vehicle));
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
}