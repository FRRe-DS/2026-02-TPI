package ar.edu.utn.frre.m3.adapter.integration;
import ar.edu.utn.frre.m3.adapter.security.DevelopmentMode;
import ar.edu.utn.frre.m3.domain.exception.IntegrationUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import java.time.*;
import java.util.*;
@Component
public class ServiceTokenProvider {
    private final RestClient client; private final DevelopmentMode mode;
    private final String uri,id,secret; private final Map<String,Cached> cache=new HashMap<>();
    private record Cached(String token,Instant until) {}
    public ServiceTokenProvider(DevelopmentMode mode,@Value("${m3.integration.token-uri:}") String uri,
            @Value("${m3.integration.client-id:}") String id,@Value("${m3.integration.client-secret:}") String secret) {
        this.mode=mode; this.uri=uri; this.id=id; this.secret=secret;
        var factory=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5)); client=RestClient.builder().requestFactory(factory).build();
    }
    /**
     * Obtiene un token técnico (client_credentials) para M3.
     *
     * @param scope scope OAuth que se pide a M1; vacío o {@code null} omite el parámetro y Keycloak
     *              entrega los scopes por defecto del cliente. Sirve para proveedores que autorizan
     *              por rol (por ejemplo M4 Grupo 03 exige el rol {@code conductores-be}) y para realms
     *              que rechazan scopes desconocidos con {@code invalid_scope}.
     */
    public synchronized String token(String scope) {
        if(uri.isBlank() && mode.enabled()) return null;
        if(uri.isBlank() || id.isBlank() || secret.isBlank()) throw new IntegrationUnavailableException("Faltan credenciales técnicas para la integración.");
        String requestedScope=scope==null ? "" : scope.trim();
        Cached existing=cache.get(requestedScope); if(existing!=null && existing.until().isAfter(Instant.now())) return existing.token();
        try {
            var form=new LinkedMultiValueMap<String,String>();form.add("grant_type","client_credentials");if(!requestedScope.isEmpty())form.add("scope",requestedScope);
            var body=client.post().uri(uri).headers(h->h.setBasicAuth(id,secret)).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
            if(body==null || body.access_token()==null || body.access_token().isBlank() || body.expires_in()<=0) throw new IllegalStateException();
            cache.put(requestedScope,new Cached(body.access_token(),Instant.now().plusSeconds(Math.max(0,body.expires_in()-30))));return body.access_token();
        } catch(RuntimeException ex) { throw new IntegrationUnavailableException("No se pudo obtener la identidad técnica de M3."); }
    }
    public record TokenResponse(String access_token,long expires_in) {}
}
