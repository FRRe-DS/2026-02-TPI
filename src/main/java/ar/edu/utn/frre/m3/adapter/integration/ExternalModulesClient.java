package ar.edu.utn.frre.m3.adapter.integration;
import ar.edu.utn.frre.m3.domain.TripContext;
import ar.edu.utn.frre.m3.domain.DeliveryResult;
import ar.edu.utn.frre.m3.domain.AvailabilityMessage;
import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.domain.exception.*;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
@Component
public class ExternalModulesClient implements M4AvailabilityPort,TripContextPort {
    private final RestClient client; private final ServiceTokenProvider tokens; private final String m4,m6;
    public ExternalModulesClient(ServiceTokenProvider tokens,@Value("${m3.integration.m4-base-url:http://localhost:4004}") String m4,
            @Value("${m3.integration.m6-base-url:http://localhost:6006}") String m6) {
        this.tokens=tokens;this.m4=m4;this.m6=m6;
        var factory=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));client=RestClient.builder().requestFactory(factory).build();
    }
    public DeliveryResult send(AvailabilityMessage m) {
        try {
            String token=tokens.token("m4:availability:write");
            var response=client.put().uri(m4+"/api/v1/drivers/{driverId}/availability-intent",m.driverId())
                .headers(h->{if(token!=null)h.setBearerAuth(token);h.set("Idempotency-Key",m.id().toString());})
                .contentType(MediaType.APPLICATION_JSON).body(m.payload()).retrieve().toEntity(Acknowledgement.class);
            var ack=response.getBody();
            return response.getStatusCode().value()==200 && ack!=null && ack.appliedVersion()==m.version() ? DeliveryResult.APPLIED : DeliveryResult.RETRY;
        } catch(RestClientResponseException ex) {
            int status=ex.getStatusCode().value();return status==429 || status>=500 || status==401 ? DeliveryResult.RETRY : DeliveryResult.REJECTED;
        } catch(RuntimeException ex) { return DeliveryResult.RETRY; }
    }
    public TripContext completedTrip(String tripId,UUID driverId) {
        try {
            String token=tokens.token("m6:rating-context:read");
            return client.get().uri(m6+"/api/v1/trips/{tripId}/rating-context?driverId={driverId}",tripId,driverId)
                .headers(h->{if(token!=null)h.setBearerAuth(token);}).retrieve().body(TripContext.class);
        } catch(RestClientResponseException ex) {
            if(ex.getStatusCode().value()==404 || ex.getStatusCode().value()==422) throw new DomainValidationException("M6 no reconoce un viaje completado para esa valoración.");
            throw new IntegrationUnavailableException("M6 no está disponible para verificar el viaje. Intentá nuevamente.");
        } catch(RestClientException ex) { throw new IntegrationUnavailableException("No se pudo conectar con M6 para verificar el viaje."); }
    }
    public record Acknowledgement(long appliedVersion) {}
}
