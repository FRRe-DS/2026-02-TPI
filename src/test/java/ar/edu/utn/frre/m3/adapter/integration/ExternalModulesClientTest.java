package ar.edu.utn.frre.m3.adapter.integration;
import ar.edu.utn.frre.m3.application.location.LocationUpdate;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.IntegrationUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ExternalModulesClientTest {
    HttpServer server; ExternalModulesClient client; AtomicInteger code=new AtomicInteger(200); AtomicReference<String> body=new AtomicReference<>("{\"appliedVersion\":7}");
    List<String> keys=new ArrayList<>(),auth=new ArrayList<>(),requestIds=new ArrayList<>();
    AtomicReference<String> method=new AtomicReference<>(),path=new AtomicReference<>(),requestBody=new AtomicReference<>();
    AtomicReference<String> contentType=new AtomicReference<>("application/json");
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1",e->{keys.add(e.getRequestHeaders().getFirst("Idempotency-Key"));auth.add(e.getRequestHeaders().getFirst("Authorization"));requestIds.add(e.getRequestHeaders().getFirst("X-Request-Id"));method.set(e.getRequestMethod());path.set(e.getRequestURI().getPath());requestBody.set(new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));byte[] data=body.get().getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type",contentType.get());e.sendResponseHeaders(code.get(),data.length);e.getResponseBody().write(data);e.close();});server.start();
        var tokens=mock(ServiceTokenProvider.class);when(tokens.token(anyString())).thenReturn("technical-test-token");
        String base="http://127.0.0.1:"+server.getAddress().getPort();client=new ExternalModulesClient(tokens,base,base,base);
    }
    @AfterEach void stop(){server.stop(0);}
    // ── T7 · Disponibilidad M3 → M4 (contrato M4 Grupo 03 v1.0.0, PUT /conductores/{id}/estado-operativo) ──

    static final String REQUESTED_AT="2026-11-28T11:15:30.123456789Z";
    static AvailabilityMessage intent(UUID driver,boolean connected,boolean available) {
        return new AvailabilityMessage(UUID.randomUUID(),driver,3,"{\"commandId\":\"ignored\",\"driverId\":\""+driver+"\",\"vehicleId\":\""+UUID.randomUUID()
            +"\",\"connected\":"+connected+",\"available\":"+available+",\"desiredVersion\":3,\"requestedAt\":\""+REQUESTED_AT+"\",\"correlationId\":\"corr-ana-1\"}",1);
    }
    static String m4Registered(UUID driver,UUID event,boolean connected,boolean available) {
        return "{\"conductor_id\":\""+driver+"\",\"event_id\":\""+event+"\",\"connected\":"+connected+",\"available\":"+available
            +",\"actualizado_en\":\"2026-11-28T11:15:30.123456Z\",\"recibido_en\":\"2026-11-28T11:15:31Z\"}";
    }
    void m4Problem(int status,String code) {
        this.code.set(status);contentType.set("application/problem+json");
        body.set("{\"type\":\"about:blank\",\"title\":\"Conflicto\",\"status\":"+status+",\"code\":\""+code+"\"}");
    }

    @Test void sendsOnlyTheFieldsOfTheM4ContractToEstadoOperativo() throws Exception {
        UUID driver=UUID.randomUUID();var m=intent(driver,true,true);
        body.set(m4Registered(driver,m.id(),true,true));code.set(201);

        assertEquals(DeliveryResult.APPLIED,client.send(m));

        assertEquals("PUT",method.get());
        assertEquals("/api/v1/conductores/"+driver+"/estado-operativo",path.get());
        var sent=new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertEquals(Set.of("event_id","connected","available","timestamp_evento"),toSet(sent.fieldNames()));
        assertEquals(m.id().toString(),sent.get("event_id").asText());
        assertTrue(sent.get("connected").asBoolean());assertTrue(sent.get("available").asBoolean());
        assertEquals("2026-11-28T11:15:30.123456Z",sent.get("timestamp_evento").asText(),"M4 (Python) recibe a lo sumo microsegundos");
        assertEquals("Bearer technical-test-token",auth.getLast());
        assertEquals("corr-ana-1",requestIds.getLast());
    }
    @Test void bothCreatedAndUpdatedProjectionsConfirmTheIntent() {
        UUID driver=UUID.randomUUID();var m=intent(driver,true,false);
        code.set(201);body.set(m4Registered(driver,m.id(),true,false));
        assertEquals(DeliveryResult.APPLIED,client.send(m),"201: primera proyección operativa en M4");
        code.set(200);
        assertEquals(DeliveryResult.APPLIED,client.send(m),"200: proyección existente actualizada");
    }
    @Test void anAnswerThatDoesNotEchoTheSentStateIsNotAConfirmation() {
        UUID driver=UUID.randomUUID();var m=intent(driver,true,true);
        body.set(m4Registered(driver,m.id(),true,false));
        assertEquals(DeliveryResult.RETRY,client.send(m),"M4 devolvió otro available");
        body.set(m4Registered(UUID.randomUUID(),m.id(),true,true));
        assertEquals(DeliveryResult.RETRY,client.send(m),"M4 respondió por otro conductor");
        code.set(202);body.set(m4Registered(driver,m.id(),true,true));
        assertEquals(DeliveryResult.RETRY,client.send(m),"202 no figura en el contrato de M4");
    }
    @Test void aDuplicateEventMeansM4AlreadyHoldsThisIntent() {
        var m=intent(UUID.randomUUID(),true,true);
        m4Problem(409,"CONFLICTO_EVENTO_ID");
        assertEquals(DeliveryResult.APPLIED,client.send(m));
    }
    @Test void anOlderEventIsRejectedWithoutRetrying() {
        var m=intent(UUID.randomUUID(),true,true);
        m4Problem(409,"CONFLICTO_TIMESTAMP");
        assertEquals(DeliveryResult.REJECTED,client.send(m));
    }
    @Test void temporaryM4FailuresAreRetriedAndDefinitiveOnesAreRejected() {
        var m=intent(UUID.randomUUID(),true,true);
        for(int status:new int[]{503,500,429,401}) { m4Problem(status,"X");assertEquals(DeliveryResult.RETRY,client.send(m),"HTTP "+status); }
        for(int status:new int[]{400,403,404,422}) { m4Problem(status,"X");assertEquals(DeliveryResult.REJECTED,client.send(m),"HTTP "+status); }
    }
    @Test void anUnreachableM4IsRetried() {
        server.stop(0);
        assertEquals(DeliveryResult.RETRY,client.send(intent(UUID.randomUUID(),true,true)));
    }
    @Test void legacyOutboxMessagesAreSentWithoutTimestampAndMalformedOnesAreRejected() throws Exception {
        UUID driver=UUID.randomUUID();
        var legacy=new AvailabilityMessage(UUID.randomUUID(),driver,1,"{\"connected\":false,\"available\":false,\"correlationId\":\"\"}",1);
        body.set(m4Registered(driver,legacy.id(),false,false));
        assertEquals(DeliveryResult.APPLIED,client.send(legacy));
        var sent=new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertEquals(Set.of("event_id","connected","available"),toSet(sent.fieldNames()));
        assertNull(requestIds.getLast(),"sin correlationId no se inventa X-Request-Id");

        int calls=auth.size();
        assertEquals(DeliveryResult.REJECTED,client.send(new AvailabilityMessage(UUID.randomUUID(),driver,1,"{\"connected\":true}",1)));
        assertEquals(calls,auth.size(),"un payload incompleto no se envía a M4");
    }
    @Test void requestsTheConfiguredScopeForM4() {
        var tokens=mock(ServiceTokenProvider.class);when(tokens.token(anyString())).thenReturn("t");
        String base="http://127.0.0.1:"+server.getAddress().getPort();
        UUID driver=UUID.randomUUID();var m=intent(driver,true,true);body.set(m4Registered(driver,m.id(),true,true));
        new ExternalModulesClient(tokens,base,base,base,"").send(m);
        verify(tokens).token("");
        new ExternalModulesClient(tokens,base,base,base,"conductores:write").send(m);
        verify(tokens).token("conductores:write");
    }
    static Set<String> toSet(Iterator<String> names) { var set=new HashSet<String>();names.forEachRemaining(set::add);return set; }

    // ── T8 · M6 (grupos 05 y 12): GET /api/v1/trips/{tripId} con identidad de servicio ──

    static String m6Trip(UUID driver,String state,String closure) {
        return "{\"id\":\"trip-42\",\"requestId\":\"r1\",\"reservationId\":null,\"clientId\":\"c1\",\"driverId\":\""+driver
            +"\",\"vehicleId\":\"v1\",\"vehicleType\":\"AUTO\",\"originLatitude\":-27.45,\"originLongitude\":-58.98,"
            +"\"destinationLatitude\":-27.4698,\"destinationLongitude\":-58.8306,\"state\":\""+state+"\",\"version\":4,"
            +"\"createdAt\":\"2026-11-28T11:00:00Z\",\"completedAt\":"+(state.equals("COMPLETED")?"\"2026-11-28T11:30:00Z\"":"null")
            +",\"closure\":"+closure+"}";
    }
    @Test void verifiesCompletedTripForTheDriverWithM6TripResource() {
        UUID driver=UUID.randomUUID();
        body.set(m6Trip(driver,"COMPLETED","{\"distanceKm\":6.4,\"durationSeconds\":900,\"finalLatitude\":-27.47,\"finalLongitude\":-58.83}"));
        var context=client.completedTrip("trip-42",driver);
        assertEquals("GET",method.get());
        assertEquals("/api/v1/trips/trip-42",path.get());
        assertEquals("c1",context.clientId());
        assertEquals("COMPLETED",context.status());
        var state=client.findTrip("trip-42").orElseThrow();
        assertEquals(-27.47,state.finalLatitude());
        assertEquals(java.time.OffsetDateTime.parse("2026-11-28T11:30:00Z"),state.completedAt());
    }
    @Test void doesNotLetTheDriverRateAnUnfinishedForeignOrUnknownTrip() {
        UUID driver=UUID.randomUUID();
        body.set(m6Trip(driver,"IN_PROGRESS","null"));
        assertThrows(ar.edu.utn.frre.m3.domain.exception.DomainValidationException.class,()->client.completedTrip("trip-42",driver));
        body.set(m6Trip(UUID.randomUUID(),"COMPLETED","null"));
        assertThrows(ar.edu.utn.frre.m3.domain.exception.DomainValidationException.class,()->client.completedTrip("trip-42",driver));
        m4Problem(404,"TRIP_NOT_FOUND");
        assertThrows(ar.edu.utn.frre.m3.domain.exception.DomainValidationException.class,()->client.completedTrip("trip-42",driver));
        assertTrue(client.findTrip("trip-42").isEmpty());
    }
    @Test void doesNotInventTripDataDuringAnM6Outage() {
        m4Problem(503,"DEPENDENCY_UNAVAILABLE");
        assertThrows(IntegrationUnavailableException.class,()->client.findTrip("trip-42"));
        assertThrows(IntegrationUnavailableException.class,()->client.completedTrip("trip-42",UUID.randomUUID()));
        server.stop(0);
        assertThrows(IntegrationUnavailableException.class,()->client.findTrip("trip-42"));
    }
    @Test void sendsLocationToTheAgreedM4ResourceWithTechnicalToken() throws Exception {
        UUID driver=UUID.randomUUID();
        var update=new LocationUpdate(driver,UUID.randomUUID(),true,"AUTO",true,true,
                -27.4511,-58.9867,OffsetDateTime.parse("2026-11-01T22:30:00-03:00"));
        client.sendLocation(update);
        assertEquals("PUT",method.get());
        assertEquals("/api/v1/conductores/"+driver+"/ubicacion",path.get());
        var sent=new com.fasterxml.jackson.databind.ObjectMapper().readTree(requestBody.get());
        assertEquals(Set.of("latitud","longitud","timestamp_ubicacion"),toSet(sent.fieldNames()),"M4 rechaza campos extra");
        assertEquals(-27.4511,sent.get("latitud").asDouble());
        assertEquals(-58.9867,sent.get("longitud").asDouble());
        assertEquals("2026-11-02T01:30:00Z",sent.get("timestamp_ubicacion").asText(),"UTC");
        assertTrue(auth.getLast().equals("Bearer technical-test-token"));
    }
    @Test void respondsToM5OfferUsingIdempotencyAndReturnsAssignedTrip() {
        UUID offer=UUID.randomUUID(),driver=UUID.randomUUID();
        body.set("{\"status\":\"ACCEPTED\",\"tripId\":\"trip-42\"}");

        var receipt=client.respondToOffer(offer,driver,"ACCEPTED");

        assertEquals("ACCEPTED",receipt.status());
        assertEquals("trip-42",receipt.tripId());
        assertEquals("POST",method.get());
        assertEquals("/api/v1/offers/"+offer+"/driver-response",path.get());
        assertNotNull(keys.getLast());
        assertTrue(requestBody.get().contains("\"driverId\":\""+driver+"\""));
        assertTrue(requestBody.get().contains("\"decision\":\"ACCEPTED\""));
    }
    @Test void m5ClosedOffersAreNotReportedAsAnOutage() {
        UUID offer=UUID.randomUUID(),driver=UUID.randomUUID();
        for(int status:new int[]{404,409,410}) {
            m4Problem(status,"X");
            assertThrows(ar.edu.utn.frre.m3.application.trip.OfferUnavailableException.class,()->client.respondToOffer(offer,driver,"ACCEPTED"),"HTTP "+status);
        }
        m4Problem(503,"X");
        assertThrows(IntegrationUnavailableException.class,()->client.respondToOffer(offer,driver,"ACCEPTED"));
        assertEquals(1,keys.stream().distinct().count(),"la misma Idempotency-Key en cada reintento");
    }
    @Test void requestsTheScopeM5RequiresAndNoScopeForM6ByDefault() {
        var tokens=mock(ServiceTokenProvider.class);when(tokens.token(anyString())).thenReturn("t");
        String base="http://127.0.0.1:"+server.getAddress().getPort();
        var defaults=new ExternalModulesClient(tokens,base,base,base);
        body.set("{\"status\":\"REJECTED\",\"tripId\":null}");
        defaults.respondToOffer(UUID.randomUUID(),UUID.randomUUID(),"REJECTED");
        verify(tokens).token("m5:offers:respond");
        body.set(m6Trip(UUID.randomUUID(),"PENDING_ARRIVAL","null"));
        defaults.findTrip("trip-42");
        verify(tokens).token("");
    }
    @Test void startsAndRequestsTripCompletionThroughM6() {
        UUID driver=UUID.randomUUID();

        client.startTrip("trip-42",driver);
        assertEquals("/api/v1/trips/trip-42/start",path.get());
        assertNotNull(keys.getLast());
        assertTrue(requestBody.get().contains("\"driverId\":\""+driver+"\""));

        client.requestTripCompletion("trip-42",driver);
        assertEquals("/api/v1/trips/trip-42/complete",path.get());
        assertNotEquals(keys.get(0),keys.get(1));
    }
}
