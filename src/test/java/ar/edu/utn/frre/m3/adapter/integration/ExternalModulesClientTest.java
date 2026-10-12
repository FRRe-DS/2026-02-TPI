package ar.edu.utn.frre.m3.adapter.integration;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.IntegrationUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ExternalModulesClientTest {
    HttpServer server; ExternalModulesClient client; AtomicInteger code=new AtomicInteger(200); AtomicReference<String> body=new AtomicReference<>("{\"appliedVersion\":7}");
    List<String> keys=new ArrayList<>(),auth=new ArrayList<>();
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/v1",e->{keys.add(e.getRequestHeaders().getFirst("Idempotency-Key"));auth.add(e.getRequestHeaders().getFirst("Authorization"));e.getRequestBody().readAllBytes();byte[] data=body.get().getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(code.get(),data.length);e.getResponseBody().write(data);e.close();});server.start();
        var tokens=mock(ServiceTokenProvider.class);when(tokens.token(anyString())).thenReturn("technical-test-token");
        String base="http://127.0.0.1:"+server.getAddress().getPort();client=new ExternalModulesClient(tokens,base,base);
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void onlyMatchingAcknowledgementAppliesAndRetriesUseStableIdentity() {
        var m=new AvailabilityMessage(UUID.randomUUID(),UUID.randomUUID(),7,"{}",1);
        assertEquals(DeliveryResult.APPLIED,client.send(m));
        body.set("{\"appliedVersion\":6}");assertEquals(DeliveryResult.RETRY,client.send(m));
        code.set(202);assertEquals(DeliveryResult.RETRY,client.send(m));
        code.set(503);assertEquals(DeliveryResult.RETRY,client.send(m));
        code.set(422);assertEquals(DeliveryResult.REJECTED,client.send(m));
        assertTrue(keys.stream().allMatch(k->k.equals(m.id().toString())));assertTrue(auth.stream().allMatch(a->a.equals("Bearer technical-test-token")));
    }
    @Test void readsTripContextAndDoesNotInventDataDuringOutage() {
        UUID driver=UUID.randomUUID();body.set("{\"tripId\":\"t1\",\"driverId\":\""+driver+"\",\"clientId\":\"c1\",\"status\":\"COMPLETED\"}");
        assertEquals(driver,client.completedTrip("t1",driver).driverId());
        code.set(503);assertThrows(IntegrationUnavailableException.class,()->client.completedTrip("t1",driver));
    }
}
