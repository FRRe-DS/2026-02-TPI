package ar.edu.utn.frre.m3.application.port;
import ar.edu.utn.frre.m3.domain.AvailabilityMessage;
import java.time.Instant;
import java.util.*;
public interface AvailabilityDeliveryPort {
    Optional<AvailabilityMessage> claim(Instant now,Instant leaseUntil);
    void finish(AvailabilityMessage message,boolean applied,boolean terminal,Instant retryAt,Instant now);
    void retry(UUID driverId,Instant now);
}
