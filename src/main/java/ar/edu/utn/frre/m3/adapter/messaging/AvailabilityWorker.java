package ar.edu.utn.frre.m3.adapter.messaging;
import ar.edu.utn.frre.m3.application.usecase.DeliverAvailabilityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="m3.integration.worker-enabled",havingValue="true",matchIfMissing=true)
public class AvailabilityWorker {
    private final DeliverAvailabilityService delivery;
    public AvailabilityWorker(DeliverAvailabilityService delivery) { this.delivery=delivery; }
    @Scheduled(fixedDelayString="${m3.integration.poll-ms:5000}",initialDelayString="${m3.integration.poll-ms:5000}")
    public void send() { delivery.deliverBatch(); }
}
