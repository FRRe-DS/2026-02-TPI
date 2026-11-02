package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.trip.DriverOffer;
import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class M5DriverOfferControllerTest {
    @Test
    void receivesDispatchOfferAndMakesItAvailableToDriverWorkflow() throws Exception {
        UUID offerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        DriverTripWorkflowService workflow = mock(DriverTripWorkflowService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new M5DriverOfferController(workflow)).build();

        mvc.perform(post("/api/v1/integration/m5/offers")
                        .contentType("application/json")
                        .content("""
                                {"offerId":"%s","driverId":"%s","requestId":"request-12",
                                 "pickupAddress":"Av. Sarmiento 1200","destinationAddress":"Av. Alberdi 800",
                                 "vehicleType":"AUTO","expiresAt":"2030-11-01T22:30:00-03:00"}
                                """.formatted(offerId, driverId)))
                .andExpect(status().isAccepted());

        verify(workflow).receiveOffer(argThat(offer -> offer.offerId().equals(offerId)
                && offer.driverId().equals(driverId)
                && offer.status() == DriverOffer.OfferStatus.PENDING
                && offer.expiresAt().toInstant().equals(OffsetDateTime.parse("2030-11-01T22:30:00-03:00").toInstant())));
    }
}
