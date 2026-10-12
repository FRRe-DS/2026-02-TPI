package ar.edu.utn.frre.m3.adapter.rest;
import ar.edu.utn.frre.m3.application.port.AvailabilityDeliveryPort;
import ar.edu.utn.frre.m3.application.usecase.DriverAccessService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class AvailabilityRetryController {
    private final AvailabilityDeliveryPort queue; private final DriverAccessService access; private final Clock clock;
    public AvailabilityRetryController(AvailabilityDeliveryPort queue,DriverAccessService access,Clock clock) { this.queue=queue;this.access=access;this.clock=clock; }
    @PostMapping("/api/v1/drivers/{driverId}/availability/retry")
    public ResponseEntity<Void> retry(@PathVariable UUID driverId) {
        access.driver(driverId);queue.retry(driverId,clock.instant());return ResponseEntity.accepted().build();
    }
}
