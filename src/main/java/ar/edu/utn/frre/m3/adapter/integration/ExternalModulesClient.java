package ar.edu.utn.frre.m3.adapter.integration;
import ar.edu.utn.frre.m3.domain.TripContext;
import ar.edu.utn.frre.m3.domain.DeliveryResult;
import ar.edu.utn.frre.m3.domain.AvailabilityMessage;
import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.application.location.LocationUpdate;
import ar.edu.utn.frre.m3.application.trip.DriverOffer;
import ar.edu.utn.frre.m3.application.trip.M6TripState;
import ar.edu.utn.frre.m3.application.trip.OfferDecisionReceipt;
import ar.edu.utn.frre.m3.application.trip.OfferUnavailableException;
import ar.edu.utn.frre.m3.domain.exception.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
@Component
public class ExternalModulesClient implements M4AvailabilityPort, M4LocationPort, M5OfferPort,
        M6TripLifecyclePort, TripContextPort {
    /** Código de problema con el que M4 (Grupo 03) informa que ese event_id ya es su estado vigente. */
    static final String M4_DUPLICATE_EVENT_CODE = "CONFLICTO_EVENTO_ID";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final RestClient client; private final ServiceTokenProvider tokens; private final String m4,m5,m6;
    /** Scope que exige M5 Grupo 04 para aceptar la respuesta del conductor (requerirScope("m5:offers:respond")). */
    static final String DEFAULT_M5_TOKEN_SCOPE = "m5:offers:respond";
    private final String m4TokenScope, m5TokenScope, m6TokenScope;
    @org.springframework.beans.factory.annotation.Autowired
    public ExternalModulesClient(ServiceTokenProvider tokens,@Value("${m3.integration.m4-base-url:http://localhost:4004}") String m4,
            @Value("${m3.integration.m5-base-url:http://localhost:5005}") String m5,
            @Value("${m3.integration.m6-base-url:http://localhost:6006}") String m6,
            @Value("${m3.integration.m4-token-scope:}") String m4TokenScope,
            @Value("${m3.integration.m5-token-scope:"+DEFAULT_M5_TOKEN_SCOPE+"}") String m5TokenScope,
            @Value("${m3.integration.m6-token-scope:}") String m6TokenScope) {
        this.tokens=tokens;this.m4=m4;this.m5=m5;this.m6=m6;
        this.m4TokenScope=m4TokenScope;this.m5TokenScope=m5TokenScope;this.m6TokenScope=m6TokenScope;
        var factory=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));client=RestClient.builder().requestFactory(factory).build();
    }
    public ExternalModulesClient(ServiceTokenProvider tokens, String m4, String m5, String m6, String m4TokenScope) {
        this(tokens, m4, m5, m6, m4TokenScope, DEFAULT_M5_TOKEN_SCOPE, "");
    }
    public ExternalModulesClient(ServiceTokenProvider tokens, String m4, String m5, String m6) {
        this(tokens, m4, m5, m6, "");
    }
    public ExternalModulesClient(ServiceTokenProvider tokens, String m4, String m6) {
        this(tokens, m4, "http://localhost:5005", m6);
    }

    /**
     * Entrega a M4 una intención de disponibilidad guardada en el outbox de M3.
     *
     * <p>Contrato: M4 Grupo 03 v1.0.0, {@code PUT /api/v1/conductores/{conductor_id}/estado-operativo}.
     * El cuerpo sólo lleva los campos que M4 acepta ({@code additionalProperties: false}):
     * {@code event_id} (id estable del comando, igual en cada reintento), {@code connected},
     * {@code available} y {@code timestamp_evento} (para que M4 descarte un estado más viejo).
     *
     * <p>Clasificación de la respuesta:
     * <ul>
     *   <li>200/201 con el mismo connected/available enviado → {@code APPLIED}.</li>
     *   <li>409 {@code CONFLICTO_EVENTO_ID} → {@code APPLIED}: M4 ya tiene ese mismo evento como vigente
     *       (por ejemplo, M3 no recibió la primera respuesta y reintentó).</li>
     *   <li>401, 429, 5xx o falla de red → {@code RETRY}.</li>
     *   <li>Otro 4xx (400, 403, 404, 409 por timestamp, 422) → {@code REJECTED}.</li>
     * </ul>
     */
    public DeliveryResult send(AvailabilityMessage m) {
        M4OperationalState state; String requestId;
        try {
            JsonNode payload=JSON.readTree(m.payload());
            state=toM4OperationalState(m.id(),payload);
            requestId=payload.path("correlationId").asText("");
        } catch(JsonProcessingException|RuntimeException malformed) {
            return DeliveryResult.REJECTED; // Un payload ilegible no mejora reintentando.
        }
        try {
            String token=tokens.token(m4TokenScope);
            var response=client.put().uri(m4+"/api/v1/conductores/{conductorId}/estado-operativo",m.driverId())
                .headers(h->{if(token!=null)h.setBearerAuth(token);if(!requestId.isBlank())h.set("X-Request-Id",requestId);})
                .contentType(MediaType.APPLICATION_JSON).body(state).retrieve().toEntity(M4OperationalStateAck.class);
            return isConfirmation(response.getStatusCode().value(),response.getBody(),m,state) ? DeliveryResult.APPLIED : DeliveryResult.RETRY;
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            if(status==409 && M4_DUPLICATE_EVENT_CODE.equals(problemCode(ex))) return DeliveryResult.APPLIED;
            return status==429 || status>=500 || status==401 ? DeliveryResult.RETRY : DeliveryResult.REJECTED;
        } catch(RuntimeException ex) { return DeliveryResult.RETRY; }
    }

    /** Traduce el payload interno del outbox (m3.availability-intent.v1) al cuerpo que acepta M4. */
    static M4OperationalState toM4OperationalState(UUID eventId,JsonNode payload) {
        if(payload==null || !payload.path("connected").isBoolean() || !payload.path("available").isBoolean())
            throw new IllegalArgumentException("El payload del outbox no tiene connected/available.");
        // Los mensajes encolados antes de este cambio no tienen requestedAt: se envían sin timestamp_evento.
        String requestedAt=payload.path("requestedAt").isTextual()
            ? Instant.parse(payload.get("requestedAt").asText()).truncatedTo(ChronoUnit.MICROS).toString() : null;
        return new M4OperationalState(eventId,payload.get("connected").asBoolean(),payload.get("available").asBoolean(),requestedAt);
    }

    private static boolean isConfirmation(int status,M4OperationalStateAck ack,AvailabilityMessage m,M4OperationalState sent) {
        if((status!=200 && status!=201) || ack==null) return false;
        if(ack.conductorId()!=null && !ack.conductorId().equals(m.driverId())) return false;
        return ack.connected()==sent.connected() && ack.available()==sent.available();
    }

    private static String problemCode(RestClientResponseException ex) {
        try { return JSON.readTree(ex.getResponseBodyAsString()).path("code").asText(null); }
        catch(JsonProcessingException|RuntimeException parseError) { return null; }
    }
    /**
     * Envía la posición del conductor a M4 Grupo 03 v1.0.0:
     * {@code PUT /api/v1/conductores/{conductor_id}/ubicacion} con {@code {latitud, longitud, timestamp_ubicacion}}
     * ({@code additionalProperties: false}). La conexión y disponibilidad viajan por {@link #send}, no aquí.
     * M4 responde 201 la primera vez y 200 después; 409 si el timestamp es anterior al vigente.
     */
    @Override
    public void sendLocation(LocationUpdate update) {
        try {
            String token=tokens.token(m4TokenScope);
            var body=new M4Location(update.latitude(),update.longitude(),
                update.recordedAt().toInstant().truncatedTo(ChronoUnit.MICROS).toString());
            client.put().uri(m4+"/api/v1/conductores/{conductorId}/ubicacion",update.driverId())
                .headers(h->{if(token!=null)h.setBearerAuth(token);})
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IntegrationUnavailableException("M4 no está disponible para recibir la ubicación.");
        }
    }
    /**
     * Respuesta del conductor a M5 Grupo 04 v1.5.0: {@code POST /api/v1/offers/{offerId}/driver-response}
     * con {@code {driverId, decision}} e {@code Idempotency-Key} estable (la misma en cada reintento).
     * 404, 409 y 410 significan que la oferta ya no admite respuesta → {@link OfferUnavailableException}.
     * 503 (M5 asignó pero no pudo crear el viaje en M6), otros errores o falla de red → reintentable.
     */
    @Override
    public OfferDecisionReceipt respondToOffer(UUID offerId, UUID driverId, String decision) {
        try {
            String token=tokens.token(m5TokenScope);
            String idempotency=UUID.nameUUIDFromBytes((offerId+":"+driverId+":"+decision).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            return client.post().uri(m5+"/api/v1/offers/{offerId}/driver-response",offerId)
                .headers(h->{if(token!=null)h.setBearerAuth(token);h.set("Idempotency-Key",idempotency);})
                .contentType(MediaType.APPLICATION_JSON).body(new M5Decision(driverId,decision))
                .retrieve().body(OfferDecisionReceipt.class);
        } catch (RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            if(status==404) throw new OfferUnavailableException("M5 no reconoce esa oferta para este conductor.");
            if(status==409) throw new OfferUnavailableException("La solicitud ya fue asignada o quedó sin disponibilidad en M5.");
            if(status==410) throw new OfferUnavailableException("La oferta venció o fue cancelada en M5.");
            throw new IntegrationUnavailableException("M5 no pudo confirmar la respuesta. Podés intentar de nuevo.");
        } catch (RestClientException ex) {
            throw new IntegrationUnavailableException("No fue posible responder la oferta en M5.");
        }
    }
    @Override
    public void startTrip(String tripId, UUID driverId) {
        sendTripAction(tripId, driverId, "start");
    }
    @Override
    public void requestTripCompletion(String tripId, UUID driverId) {
        sendTripAction(tripId, driverId, "complete");
    }
    private void sendTripAction(String tripId, UUID driverId, String action) {
        try {
            String token=tokens.token(m6TokenScope);
            String idempotency=UUID.nameUUIDFromBytes((tripId+":"+action).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            client.post().uri(m6+"/api/v1/trips/{tripId}/{action}",tripId,action)
                .headers(h->{if(token!=null)h.setBearerAuth(token);h.set("Idempotency-Key",idempotency);})
                .contentType(MediaType.APPLICATION_JSON).body(new M6TripAction(driverId))
                .retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IntegrationUnavailableException(action.equals("start")
                    ? "No fue posible iniciar el viaje en M6." : "No fue posible solicitar la finalización en M6.");
        }
    }
    /**
     * Verifica en M6 que el viaje esté completado y sea de este conductor antes de que valore al cliente (RF-3.7).
     * M6 (grupos 05 y 12) no tiene {@code /rating-context}: se lee {@code GET /api/v1/trips/{tripId}}, que permite el
     * rol de servicio (SYSTEM/SERVICIO), y se exigen {@code state=COMPLETED} y el mismo {@code driverId}.
     */
    @Override
    public TripContext completedTrip(String tripId,UUID driverId) {
        M6TripState trip=findTrip(tripId).orElseThrow(() -> new DomainValidationException("M6 no reconoce ese viaje."));
        if(!driverId.equals(trip.driverId())) throw new DomainValidationException("M6 no asigna ese viaje a este conductor.");
        if(!trip.isCompleted()) throw new DomainValidationException("El viaje todavía no está completado en M6.");
        return new TripContext(trip.tripId(),trip.driverId(),trip.clientId(),"COMPLETED");
    }

    /** {@code GET /api/v1/trips/{tripId}} de M6 con identidad de servicio; vacío ante 404. */
    @Override
    public Optional<M6TripState> findTrip(String tripId) {
        M6Trip trip;
        try {
            String token=tokens.token(m6TokenScope);
            trip=client.get().uri(m6+"/api/v1/trips/{tripId}",tripId)
                .headers(h->{if(token!=null)h.setBearerAuth(token);}).retrieve().body(M6Trip.class);
        } catch(RestClientResponseException ex) {
            if(ex.getStatusCode().value()==404) return Optional.empty();
            throw new IntegrationUnavailableException("M6 no está disponible para consultar el viaje. Intentá nuevamente.");
        } catch(RestClientException ex) {
            throw new IntegrationUnavailableException("No se pudo conectar con M6 para consultar el viaje.");
        }
        if(trip==null || trip.id()==null || trip.state()==null) throw new IntegrationUnavailableException("M6 devolvió un viaje incompleto.");
        M6Closure closure=trip.closure();
        return Optional.of(new M6TripState(trip.id(),trip.driverId(),trip.clientId(),trip.state(),
            closure==null?null:closure.finalLatitude(),closure==null?null:closure.finalLongitude(),
            trip.destinationLatitude(),trip.destinationLongitude(),trip.completedAt()));
    }
    /** Cuerpo de {@code EstadoOperativo} en el contrato de M4 Grupo 03 (snake_case, sin campos extra). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record M4OperationalState(@JsonProperty("event_id") UUID eventId, boolean connected, boolean available,
            @JsonProperty("timestamp_evento") String timestampEvento) {}
    /** Cuerpo de {@code ActualizarUbicacion} en el contrato de M4 Grupo 03. */
    public record M4Location(double latitud, double longitud, @JsonProperty("timestamp_ubicacion") String timestampUbicacion) {}
    /** Parte de {@code EstadoOperativoRegistrado} que M3 usa para confirmar la entrega. */
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record M4OperationalStateAck(@JsonProperty("conductor_id") UUID conductorId, boolean connected, boolean available) {}
    public record M5Decision(UUID driverId, String decision) {}
    public record M6TripAction(UUID driverId) {}
    /** Campos de {@code Trip} (contrato M6) que usa M3; el resto se ignora. */
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record M6Trip(String id, UUID driverId, String clientId, String state, Double destinationLatitude,
            Double destinationLongitude, java.time.OffsetDateTime completedAt, M6Closure closure) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record M6Closure(Double finalLatitude, Double finalLongitude) {}
}
