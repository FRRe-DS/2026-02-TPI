package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.usecase.DriverOperationalStatusService;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.DriverOperationalState;
import ar.edu.utn.frre.m3.domain.Vehicle;
import ar.edu.utn.frre.m3.domain.exception.VehicleNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecturas del contrato M3 2.0.0 (igual en 3.0.0) que consume M4 al validar candidatos:
 * <ul>
 *   <li>{@code GET /api/v1/conductores/{id_conductor}/estado}</li>
 *   <li>{@code GET /api/v1/conductores/{id_conductor}/ubicacion}</li>
 *   <li>{@code GET /api/v1/conductores/{id_conductor}/vehiculos/{id_vehiculo}}</li>
 * </ul>
 * Sólo traduce al vocabulario del contrato (snake_case y valores en español). La regla de qué estado
 * corresponde vive en {@link DriverOperationalStatusService}. Quién puede leer lo decide
 * {@code DriverAuthorizationFilter}: el propio conductor, un operador o una identidad de servicio.
 *
 * <p>Errores: {@code application/problem+json} (RFC 9457, AGENTS.md) con los miembros adicionales
 * {@code codigo} y {@code mensaje} que define el esquema {@code Error} del contrato.
 */
@RestController
@RequestMapping("/api/v1/conductores/{idConductor}")
public class DriverStateContractController {

    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Map<String, String> QUALIFICATION = Map.of(
            "PENDING", "PENDIENTE", "APPROVED", "APROBADA", "OBSERVED", "OBSERVADA",
            "SUSPENDED", "SUSPENDIDA", "REJECTED", "RECHAZADA");
    private static final Map<DriverOperationalState, String> STATE = Map.of(
            DriverOperationalState.OFFLINE, "DESCONECTADO", DriverOperationalState.CONNECTED, "CONECTADO",
            DriverOperationalState.AVAILABLE, "DISPONIBLE", DriverOperationalState.BUSY, "OCUPADO");

    private final DriverOperationalStatusService queries;
    private final Clock clock;

    public DriverStateContractController(DriverOperationalStatusService queries, Clock clock) {
        this.queries = queries;
        this.clock = clock;
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoConductor> estado(@PathVariable UUID idConductor) {
        var status = queries.status(idConductor);
        return ResponseEntity.ok().eTag(String.valueOf(status.driverVersion()))
                .body(new EstadoConductor(status.driverId(), STATE.get(status.state()), status.activeVehicleId(), utc(status.updatedAt())));
    }

    @GetMapping("/ubicacion")
    public UbicacionConductor ubicacion(@PathVariable UUID idConductor) {
        var location = queries.location(idConductor);
        return new UbicacionConductor(location.driverId(), location.latitude(), location.longitude(),
                location.recordedAt() == null ? null : utc(location.recordedAt()));
    }

    @GetMapping("/vehiculos/{idVehiculo}")
    public Vehiculo vehiculo(@PathVariable UUID idConductor, @PathVariable UUID idVehiculo) {
        var view = queries.vehicle(idConductor, idVehiculo);
        Vehicle v = view.vehicle();
        boolean approved = "APPROVED".equals(v.getQualificationStatus());
        Instant now = clock.instant();
        return new Vehiculo(v.getId(), v.getDriverId(), v.getVehicleType(), v.getPlate(), v.getMake(), v.getModel(),
                v.getModelYear(), serviceTypes(v.getServiceCategories(), approved),
                view.currentDocuments().stream().map(d -> document(d, now)).toList(),
                QUALIFICATION.getOrDefault(v.getQualificationStatus(), v.getQualificationStatus()));
    }

    /** STANDARD, PREMIUM y PETS transportan pasajeros; DELIVERY corresponde a envíos. */
    static List<TipoServicioHabilitado> serviceTypes(Set<String> categories, boolean approved) {
        if (categories == null) return List.of();
        var types = new LinkedHashSet<String>();
        for (String category : categories) types.add("DELIVERY".equals(category) ? "ENVIOS" : "PASAJEROS");
        return types.stream().map(type -> new TipoServicioHabilitado(type, approved)).toList();
    }

    private static DocumentoVehiculo document(Document d, Instant now) {
        String type = d.getDocumentType() == DocumentType.INSURANCE ? "SEGURO" : "CEDULA_VERDE";
        String url = d.getStorageReference() != null && d.getStorageReference().matches("(?i)^https?://.*") ? d.getStorageReference() : null;
        return new DocumentoVehiculo(d.getVehicleId(), type, d.getDocumentNumber(), d.getIssuingEntity(),
                LocalDate.ofInstant(d.getIssuedAt(), ARGENTINA), LocalDate.ofInstant(d.getValidUntil(), ARGENTINA),
                url, d.isExpired(now) ? "VENCIDO" : "VIGENTE");
    }

    private static String utc(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    @ExceptionHandler({ar.edu.utn.frre.m3.application.exception.DriverNotFoundException.class,
            ar.edu.utn.frre.m3.domain.exception.DriverNotFoundException.class, VehicleNotFoundException.class})
    public ResponseEntity<ProblemDetail> notFound(RuntimeException ex, HttpServletRequest request) {
        String message = ex instanceof VehicleNotFoundException
                ? "El vehículo indicado no existe para ese conductor." : "El conductor indicado no existe.";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
        problem.setTitle("No encontrado");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("codigo", "NO_ENCONTRADO");
        problem.setProperty("mensaje", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }

    public record EstadoConductor(@JsonProperty("id_conductor") UUID idConductor, String estado,
            @JsonProperty("id_vehiculo_activo") UUID idVehiculoActivo, String actualizado) {}

    public record UbicacionConductor(@JsonProperty("id_conductor") UUID idConductor, Double latitud, Double longitud,
            @JsonProperty("fecha_act_ubic") String fechaActUbic) {}

    public record TipoServicioHabilitado(String tipo, boolean habilitado) {}

    public record DocumentoVehiculo(@JsonProperty("id_vehiculo") UUID idVehiculo, String tipo, String numero,
            @JsonProperty("entidad_emisora") String entidadEmisora, @JsonProperty("fecha_emision") LocalDate fechaEmision,
            @JsonProperty("fecha_vencimiento") LocalDate fechaVencimiento, @JsonProperty("url_archivo") String urlArchivo,
            String estado) {}

    public record Vehiculo(UUID id, @JsonProperty("id_conductor") UUID idConductor, String tipo, String patente,
            String marca, String modelo, Integer anio, @JsonProperty("tipos_serv") List<TipoServicioHabilitado> tiposServ,
            List<DocumentoVehiculo> documentos, String habilitacion) {}
}
