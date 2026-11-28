package ar.edu.utn.frre.m3.application.port;

import ar.edu.utn.frre.m3.application.trip.M6TripState;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida hacia M6 (viajes y ciclo de vida). */
public interface M6TripLifecyclePort {
    void startTrip(String tripId, UUID driverId);
    void requestTripCompletion(String tripId, UUID driverId);

    /**
     * Estado actual del viaje en M6, leído con identidad de servicio. Vacío si M6 no lo conoce (404).
     * Lanza {@code IntegrationUnavailableException} si M6 no responde.
     */
    Optional<M6TripState> findTrip(String tripId);
}
