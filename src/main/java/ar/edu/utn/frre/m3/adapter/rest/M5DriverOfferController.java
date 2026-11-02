package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.M5DriverOfferRequest;
import ar.edu.utn.frre.m3.application.trip.DriverOffer;
import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives an offer already dispatched by M5; M3 does not perform dispatch. */
@RestController
@RequestMapping("/api/v1/integration/m5/offers")
public class M5DriverOfferController {
    private final DriverTripWorkflowService workflow;

    public M5DriverOfferController(DriverTripWorkflowService workflow) {
        this.workflow = workflow;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody M5DriverOfferRequest request) {
        workflow.receiveOffer(new DriverOffer(request.offerId(), request.driverId(), request.requestId(),
                request.pickupAddress(), request.destinationAddress(), request.vehicleType(), request.expiresAt(),
                DriverOffer.OfferStatus.PENDING, null));
        return ResponseEntity.accepted().build();
    }
}
