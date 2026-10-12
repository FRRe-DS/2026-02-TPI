package ar.edu.utn.frre.m3.application.usecase;
import ar.edu.utn.frre.m3.domain.DeliveryResult;
import ar.edu.utn.frre.m3.application.port.*;
import java.time.Clock;
public class DeliverAvailabilityService {
    private final AvailabilityDeliveryPort queue; private final M4AvailabilityPort m4; private final Clock clock;
    public DeliverAvailabilityService(AvailabilityDeliveryPort queue,M4AvailabilityPort m4,Clock clock) { this.queue=queue; this.m4=m4; this.clock=clock; }
    public void deliverBatch() {
        for(int i=0;i<20;i++) {
            var now=clock.instant(); var claimed=queue.claim(now,now.plusSeconds(60));
            if(claimed.isEmpty()) return;
            var message=claimed.get(); DeliveryResult result;
            try { result=m4.send(message); } catch(RuntimeException ex) { result=DeliveryResult.RETRY; }
            boolean applied=result==DeliveryResult.APPLIED;
            boolean terminal=result==DeliveryResult.REJECTED || message.attempt()>=6;
            var finished=clock.instant();
            queue.finish(message,applied,terminal,finished.plusSeconds(Math.min(60,1L << Math.min(message.attempt(),6))),finished);
        }
    }
}
