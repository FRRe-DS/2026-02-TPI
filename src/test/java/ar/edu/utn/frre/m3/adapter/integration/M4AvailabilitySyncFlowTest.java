package ar.edu.utn.frre.m3.adapter.integration;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.usecase.DeliverAvailabilityService;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.ReviewDecision;
import ar.edu.utn.frre.m3.domain.ServiceType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T7 · Recorrido completo de una intención de disponibilidad con un M4 <strong>simulado</strong>.
 *
 * <p>Corre M3 real (controlador, caso de uso, outbox en H2, worker y cliente HTTP) contra un servidor
 * HTTP local que imita el comportamiento publicado por M4 Grupo 03 (commit 35e597e, openapi 1.0.0):
 * {@code PUT /api/v1/conductores/{id}/estado-operativo}, campos extra → 422, mismo {@code event_id}
 * que el vigente → 409 {@code CONFLICTO_EVENTO_ID}, {@code timestamp_evento} anterior → 409
 * {@code CONFLICTO_TIMESTAMP}, 201 la primera vez y 200 después. No reemplaza la prueba con el M4 real.
 */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class M4AvailabilitySyncFlowTest {

    static final FakeM4 M4 = FakeM4.start();

    @DynamicPropertySource
    static void m4Url(DynamicPropertyRegistry registry) {
        registry.add("m3.integration.m4-base-url", M4::baseUrl);
    }

    @Autowired MockMvc mvc;
    @Autowired SpringDataDriverRepository drivers;
    @Autowired SpringDataVehicleRepository vehicles;
    @Autowired DocumentRepository documents;
    @Autowired DeliverAvailabilityService delivery;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entities;
    @MockBean ServiceTokenProvider tokens;

    UUID driver, vehicle;
    String subject, path;

    @BeforeEach
    void anaIsQualifiedWithHerCarAndCurrentDocuments() throws Exception {
        M4.reset();
        when(tokens.token(anyString())).thenReturn("token-tecnico-de-prueba");
        driver = UUID.randomUUID(); vehicle = UUID.randomUUID();
        subject = "ana-" + driver; path = "/api/v1/drivers/" + driver;
        Instant now = Instant.now();
        drivers.saveAndFlush(new DriverJpaEntity(driver, subject, "Ana", "PENDING", 1L, now, now));
        vehicles.saveAndFlush(new VehicleJpaEntity(vehicle, driver, "AUTO", "AUTO", "AR", "AA123BB", "Fiat", "Cronos", 2023, "PENDING", 1L, now, now));
        var license = Document.createPendingLicense(driver, "LIC-ANA", now.minusSeconds(60), now.plusSeconds(86400), Set.of(ServiceType.AUTO), null, 1, 1);
        license.review(ReviewDecision.APPROVED, "Verificado", "operator", 1); documents.save(license);
        for (var type : List.of(DocumentType.INSURANCE, DocumentType.VEHICLE_REGISTRATION)) {
            var doc = Document.createPendingRevision(driver, vehicle, type, "DOC-ANA", "Test", now.minusSeconds(60), now.plusSeconds(86400), null, 1);
            doc.review(ReviewDecision.APPROVED, "Verificado", "operator", 1); documents.save(doc);
        }
        entities.flush();
        approve("/qualification", 1);
        approve("/vehicles/" + vehicle + "/qualification", 2);
    }

    void approve(String suffix, long version) throws Exception {
        mvc.perform(post(path + suffix).with(user("operator").roles("OPERATOR")).header("If-Match", "\"" + version + "\"")
                .header("Idempotency-Key", UUID.randomUUID().toString()).contentType("application/json")
                .content("{\"status\":\"APPROVED\",\"reason\":\"Verificación de prueba\"}")).andExpect(status().isOk());
    }

    void anaRequests(boolean connected, boolean available) throws Exception {
        mvc.perform(put(path + "/availability-intent").with(user(subject)).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json")
                .content("{\"vehicleId\":\"" + vehicle + "\",\"connected\":" + connected + ",\"available\":" + available + "}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"));
        entities.flush();
    }

    void workerRuns() {
        delivery.deliverBatch();
        entities.clear();
    }

    /** El worker espera antes de reintentar; en la prueba adelantamos ese momento en vez de dormir. */
    void retryDelayElapses() {
        jdbc.update("update outbox set next_attempt_at=? where status='PENDING'", java.sql.Timestamp.from(Instant.now().minusSeconds(1)));
    }

    String m3SyncStatus() throws Exception {
        String json = mvc.perform(get(path + "/availability").with(user(subject))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(json).get("status").asText();
    }

    @Test
    void anaConnectsAndM4ConfirmsTheIntent() throws Exception {
        anaRequests(true, true);

        workerRuns();

        mvc.perform(get(path + "/availability").with(user(subject)))
            .andExpect(jsonPath("$.status").value("APPLIED")).andExpect(jsonPath("$.appliedVersion").value(1));
        var received = M4.requests.getFirst();
        assertEquals("/api/v1/conductores/" + driver + "/estado-operativo", received.path());
        assertEquals("Bearer token-tecnico-de-prueba", received.authorization());
        assertEquals(Set.of("event_id", "connected", "available", "timestamp_evento"), fieldNames(received.body()));
        assertEquals(201, received.status(), "primera proyección operativa del conductor en M4");
        assertTrue(M4.state(driver).get("available").asBoolean());
    }

    @Test
    void m4IsDownThenRecoversAndTheSameEventIsDelivered() throws Exception {
        anaRequests(true, true);
        M4.mode = Mode.DOWN;

        workerRuns();

        assertEquals("RETRYING", m3SyncStatus());
        assertEquals("PENDING", jdbc.queryForObject("select status from outbox where aggregate_id=(select id from availability_intents where driver_id=?)", String.class, driver));
        assertNull(M4.state(driver), "M4 no guardó nada");

        M4.mode = Mode.NORMAL;
        retryDelayElapses();
        workerRuns();

        assertEquals("APPLIED", m3SyncStatus());
        assertEquals(2, M4.requests.size());
        assertEquals(M4.requests.get(0).eventId(), M4.requests.get(1).eventId(), "el reintento conserva event_id");
    }

    @Test
    void m4SavedTheStateButItsAnswerWasLostSoTheRetryIsRecognisedAsDuplicate() throws Exception {
        anaRequests(true, true);
        M4.mode = Mode.SAVE_THEN_FAIL;

        workerRuns();
        assertEquals("RETRYING", m3SyncStatus());
        assertNotNull(M4.state(driver), "M4 sí aplicó el cambio");

        M4.mode = Mode.NORMAL;
        retryDelayElapses();
        workerRuns();

        assertEquals(409, M4.requests.getLast().status(), "M4 contesta CONFLICTO_EVENTO_ID");
        assertEquals("APPLIED", m3SyncStatus());
    }

    @Test
    void anaConnectsAndDisconnectsBeforeDeliveryAndM4EndsWithTheLatestState() throws Exception {
        anaRequests(true, true);
        anaRequests(false, false);

        workerRuns();

        assertEquals(2, M4.requests.size());
        assertFalse(M4.state(driver).get("connected").asBoolean(), "el último pedido de Ana queda vigente en M4");
        mvc.perform(get(path + "/availability").with(user(subject)))
            .andExpect(jsonPath("$.status").value("APPLIED")).andExpect(jsonPath("$.desiredVersion").value(2))
            .andExpect(jsonPath("$.appliedVersion").value(2));
    }

    /**
     * Las consultas que hace M4 Grupo 03 en candidatosCercanos.py (_verificar_candidato_m3) contra el contrato
     * M3 2.0.0: primero el estado, después el vehículo activo. Usa la identidad de servicio de M4 (rol ubicacion-be).
     */
    @Test
    void m4ValidatesAnaAsCandidateThroughTheM3Contract() throws Exception {
        var m4 = user("service-account-grupo-03").roles("UBICACION-BE");
        String conductor = "/api/v1/conductores/" + driver;
        mvc.perform(get(conductor + "/estado").with(m4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("DESCONECTADO")).andExpect(jsonPath("$.id_vehiculo_activo").doesNotExist());

        anaRequests(true, true);

        mvc.perform(get(conductor + "/estado").with(m4)).andExpect(status().isOk())
            .andExpect(header().exists("ETag"))
            .andExpect(jsonPath("$.id_conductor").value(driver.toString()))
            .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.id_vehiculo_activo").value(vehicle.toString()))
            .andExpect(jsonPath("$.actualizado").value(org.hamcrest.Matchers.endsWith("Z")));
        mvc.perform(get(conductor + "/vehiculos/" + vehicle).with(m4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(vehicle.toString()))
            .andExpect(jsonPath("$.id_conductor").value(driver.toString()))
            .andExpect(jsonPath("$.tipo").value("AUTO"))
            .andExpect(jsonPath("$.habilitacion").value("APROBADA"))
            .andExpect(jsonPath("$.patente").value("AA123BB"))
            .andExpect(jsonPath("$.documentos.length()").value(2))
            .andExpect(jsonPath("$.documentos[0].estado").value("VIGENTE"));
        mvc.perform(get(conductor + "/ubicacion").with(m4)).andExpect(status().isOk())
            .andExpect(jsonPath("$.id_conductor").value(driver.toString()))
            .andExpect(jsonPath("$.latitud").isEmpty()).andExpect(jsonPath("$.fecha_act_ubic").isEmpty());
        mvc.perform(get(conductor + "/vehiculos/" + UUID.randomUUID()).with(m4)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));

        anaRequests(true, false);
        mvc.perform(get(conductor + "/estado").with(m4)).andExpect(jsonPath("$.estado").value("CONECTADO"));
        anaRequests(false, false);
        mvc.perform(get(conductor + "/estado").with(m4)).andExpect(jsonPath("$.estado").value("DESCONECTADO"));

        mvc.perform(get(conductor + "/estado").with(user("otra-conductora").roles("DRIVER"))).andExpect(status().isForbidden());
        mvc.perform(get(conductor + "/estado").with(user(subject).roles("DRIVER"))).andExpect(status().isOk());
    }

    static Set<String> fieldNames(JsonNode node) {
        var names = new HashSet<String>(); node.fieldNames().forEachRemaining(names::add); return names;
    }

    enum Mode { NORMAL, DOWN, SAVE_THEN_FAIL }

    record Received(String path, String authorization, JsonNode body, String eventId, int status) {}

    /** Servidor HTTP mínimo con las reglas de estado-operativo del M4 Grupo 03. */
    static final class FakeM4 {
        static final Set<String> ALLOWED = Set.of("event_id", "connected", "available", "timestamp_evento");
        final HttpServer server;
        final ObjectMapper json = new ObjectMapper();
        final Map<String, JsonNode> states = new ConcurrentHashMap<>();
        final List<Received> requests = new CopyOnWriteArrayList<>();
        volatile Mode mode = Mode.NORMAL;

        private FakeM4(HttpServer server) { this.server = server; }

        static FakeM4 start() {
            try {
                var fake = new FakeM4(HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0));
                fake.server.createContext("/api/v1/conductores/", fake::handle);
                fake.server.start();
                return fake;
            } catch (IOException ex) { throw new IllegalStateException(ex); }
        }

        String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
        void reset() { states.clear(); requests.clear(); mode = Mode.NORMAL; }
        JsonNode state(UUID driver) { return states.get(driver.toString()); }

        void handle(HttpExchange e) throws IOException {
            String[] parts = e.getRequestURI().getPath().split("/");   // ["", api, v1, conductores, {id}, estado-operativo]
            String auth = e.getRequestHeaders().getFirst("Authorization");
            JsonNode body = json.readTree(e.getRequestBody().readAllBytes());
            String eventId = body.path("event_id").asText(null);
            int status;
            String response;
            if (!"PUT".equals(e.getRequestMethod()) || parts.length != 6 || !"estado-operativo".equals(parts[5])) {
                status = 404; response = problem(404, "RECURSO_NO_DISPONIBLE");
            } else if (auth == null || !auth.startsWith("Bearer ")) {
                status = 401; response = problem(401, "NO_AUTENTICADO");
            } else if (!ALLOWED.containsAll(fieldNames(body)) || !body.path("connected").isBoolean() || !body.path("available").isBoolean()) {
                status = 422; response = problem(422, "NO_PROCESABLE");
            } else if (mode == Mode.DOWN) {
                status = 503; response = problem(503, "DEPENDENCIA_NO_DISPONIBLE");
            } else {
                String driver = parts[4];
                JsonNode current = states.get(driver);
                if (current != null && eventId != null && eventId.equals(current.path("event_id").asText(null))) {
                    status = 409; response = problem(409, "CONFLICTO_EVENTO_ID");
                } else if (current != null && body.hasNonNull("timestamp_evento") && current.hasNonNull("timestamp_evento")
                        && Instant.parse(body.get("timestamp_evento").asText()).isBefore(Instant.parse(current.get("timestamp_evento").asText()))) {
                    status = 409; response = problem(409, "CONFLICTO_TIMESTAMP");
                } else {
                    boolean connected = body.get("connected").asBoolean();
                    var saved = json.createObjectNode().put("conductor_id", driver).put("event_id", eventId)
                        .put("connected", connected).put("available", connected && body.get("available").asBoolean())
                        .put("timestamp_evento", body.path("timestamp_evento").asText(null))
                        .put("recibido_en", Instant.now().toString());
                    states.put(driver, saved);
                    status = mode == Mode.SAVE_THEN_FAIL ? 500 : current == null ? 201 : 200;
                    response = status == 500 ? problem(500, "ERROR_INTERNO") : saved.toString();
                }
            }
            requests.add(new Received(e.getRequestURI().getPath(), auth, body, eventId, status));
            byte[] data = response.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
            e.sendResponseHeaders(status, data.length);
            e.getResponseBody().write(data);
            e.close();
        }

        static String problem(int status, String code) {
            return "{\"type\":\"about:blank\",\"title\":\"Problema\",\"status\":" + status + ",\"code\":\"" + code + "\"}";
        }
    }
}
