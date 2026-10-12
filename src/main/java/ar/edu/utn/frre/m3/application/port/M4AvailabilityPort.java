package ar.edu.utn.frre.m3.application.port;
import ar.edu.utn.frre.m3.domain.DeliveryResult;
import ar.edu.utn.frre.m3.domain.AvailabilityMessage;
public interface M4AvailabilityPort {
    DeliveryResult send(AvailabilityMessage message);
}
