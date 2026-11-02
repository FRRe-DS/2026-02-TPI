package ar.edu.utn.frre.m3.application.port;

import java.util.UUID;

/** Outbound M6 port for starting and requesting completion of an assigned trip. */
public interface M6TripLifecyclePort {
    void startTrip(String tripId, UUID driverId);
    void requestTripCompletion(String tripId, UUID driverId);
}
