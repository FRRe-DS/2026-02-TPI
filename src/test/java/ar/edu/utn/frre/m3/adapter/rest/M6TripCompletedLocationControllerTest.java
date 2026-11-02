package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.location.GeoPosition;
import ar.edu.utn.frre.m3.application.trip.DriverTripWorkflowService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class M6TripCompletedLocationControllerTest {
    @Test
    void completedTripDestinationIsPassedToTheSimulator() throws Exception {
        UUID driverId = UUID.fromString("f1f8e742-443d-4fd2-97f3-fadf74db7c8c");
        DriverTripWorkflowService workflow = mock(DriverTripWorkflowService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new M6TripCompletedLocationController(workflow)).build();

        mvc.perform(post("/api/v1/integration/m6/trips/completed")
                        .contentType("application/json")
                        .content("""
                                {"tripId":"trip-42","driverId":"%s","status":"FINALIZADO",
                                 "destination":{"latitude":-27.4698,"longitude":-58.8306}}
                                """.formatted(driverId)))
                .andExpect(status().isAccepted());

        verify(workflow).completedByM6(eq("trip-42"), eq(driverId),
                eq(new GeoPosition(-27.4698, -58.8306)), eq(null), eq(null));
    }
}
