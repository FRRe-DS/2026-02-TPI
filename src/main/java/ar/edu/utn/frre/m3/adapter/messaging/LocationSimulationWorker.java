package ar.edu.utn.frre.m3.adapter.messaging;

import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "location-simulator.enabled", havingValue = "true", matchIfMissing = true)
public class LocationSimulationWorker {
    private final LocationSimulationService simulation;

    public LocationSimulationWorker(LocationSimulationService simulation) {
        this.simulation = simulation;
    }

    @Scheduled(fixedDelayString = "${location-simulator.interval-ms:5000}",
            initialDelayString = "${location-simulator.interval-ms:5000}")
    public void publish() {
        simulation.publishConnectedLocations();
    }
}
