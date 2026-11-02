package ar.edu.utn.frre.m3.config;

import ar.edu.utn.frre.m3.application.location.LocationSimulationService;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.M4LocationPort;
import ar.edu.utn.frre.m3.application.port.M5OfferPort;
import ar.edu.utn.frre.m3.application.port.M6TripLifecyclePort;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocationSimulationConfig {
    @Bean
    public LocationSimulationService locationSimulationService(AvailabilityIntentRepository intents,
            DriverRepository drivers, VehicleRepository vehicles, M4LocationPort m4, Clock clock,
            @Value("${location-simulator.max-step-meters:35}") double maxStepMeters) {
        return new LocationSimulationService(intents, drivers, vehicles, m4, clock, maxStepMeters);
    }

    @Bean
    public DriverTripWorkflowService driverTripWorkflowService(DriverRepository drivers,
            AvailabilityIntentRepository availability, M5OfferPort m5, M6TripLifecyclePort m6,
            LocationSimulationService locations, Clock clock) {
        return new DriverTripWorkflowService(drivers, availability, m5, m6, locations, clock);
    }
}
