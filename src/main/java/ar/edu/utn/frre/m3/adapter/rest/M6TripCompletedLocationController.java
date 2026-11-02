package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.TripCompletedLocationRequest;
import ar.edu.utn.frre.m3.application.location.GeoPosition;
import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives M6's trip-completed notification and moves the simulator to its destination. */
@RestController
@RequestMapping("/api/v1/integration/m6/trips")
public class M6TripCompletedLocationController {
    private final DriverTripWorkflowService workflow;

    public M6TripCompletedLocationController(DriverTripWorkflowService workflow) {
        this.workflow = workflow;
    }

    @PostMapping("/completed")
    public ResponseEntity<Void> completed(@Valid @RequestBody TripCompletedLocationRequest request) {
        if (!"FINALIZADO".equals(request.status())) {
            throw new IllegalArgumentException("M6 debe informar el estado FINALIZADO.");
        }
        workflow.completedByM6(request.tripId(), request.driverId(),
                new GeoPosition(request.destination().latitude(), request.destination().longitude()),
                request.driverRating(), request.completedAt());
        return ResponseEntity.accepted().build();
    }
}
