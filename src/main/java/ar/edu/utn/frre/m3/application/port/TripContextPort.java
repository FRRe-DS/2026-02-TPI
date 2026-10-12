package ar.edu.utn.frre.m3.application.port;
import ar.edu.utn.frre.m3.domain.TripContext;
import java.util.UUID;
public interface TripContextPort {
    TripContext completedTrip(String tripId, UUID driverId);
}
