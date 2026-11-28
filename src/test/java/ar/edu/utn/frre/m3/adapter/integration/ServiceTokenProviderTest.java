package ar.edu.utn.frre.m3.adapter.integration;

import ar.edu.utn.frre.m3.adapter.security.DevelopmentMode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T7 · Cómo pide M3 su token técnico a M1 (client_credentials). */
class ServiceTokenProviderTest {

    HttpServer keycloak;
    List<String> forms = new ArrayList<>();
    ServiceTokenProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        keycloak = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        keycloak.createContext("/token", e -> {
            forms.add(URLDecoder.decode(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            byte[] data = ("{\"access_token\":\"token-" + forms.size() + "\",\"expires_in\":300}").getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, data.length);
            e.getResponseBody().write(data);
            e.close();
        });
        keycloak.start();
        var mode = mock(DevelopmentMode.class);
        provider = new ServiceTokenProvider(mode, "http://127.0.0.1:" + keycloak.getAddress().getPort() + "/token",
                "grupo-02", "secreto-de-prueba");
    }

    @AfterEach
    void stop() {
        keycloak.stop(0);
    }

    @Test
    void blankScopeIsNotSentSoKeycloakAppliesTheClientDefaults() {
        assertEquals("token-1", provider.token(""));
        assertEquals("token-1", provider.token(null), "vacío y null comparten el token en caché");
        assertEquals(1, forms.size());
        assertEquals("grant_type=client_credentials", forms.getFirst());
    }

    @Test
    void explicitScopeIsRequestedAndCachedSeparately() {
        assertEquals("token-1", provider.token("conductores:write"));
        assertEquals("token-2", provider.token(""));
        assertEquals("token-1", provider.token("conductores:write"));
        assertEquals(2, forms.size());
        assertTrue(forms.getFirst().contains("scope=conductores:write"));
    }
}
