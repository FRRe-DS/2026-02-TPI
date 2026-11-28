package ar.edu.utn.frre.m3.adapter.messaging;

import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Consulta periódicamente a M6 el estado de los viajes activos (T8).
 *
 * <p>M6 comunica el fin y la cancelación con eventos ({@code TripCompleted}, {@code TripCancelled}) y no llama
 * a M3 por HTTP. Hasta acordar el consumo de esos eventos, este proceso lee {@code GET /api/v1/trips/{tripId}}
 * para liberar al conductor cuando M6 cierra el viaje. Se desactiva con {@code M6_SYNC_ENABLED=false}.
 */
@Component
@ConditionalOnProperty(name = "m3.integration.m6-sync-enabled", havingValue = "true", matchIfMissing = true)
public class TripStatusWorker {
    private final DriverTripWorkflowService workflow;

    public TripStatusWorker(DriverTripWorkflowService workflow) {
        this.workflow = workflow;
    }

    @Scheduled(fixedDelayString = "${m3.integration.m6-sync-ms:10000}", initialDelayString = "${m3.integration.m6-sync-ms:10000}")
    public void sync() {
        workflow.syncWithM6();
    }
}
