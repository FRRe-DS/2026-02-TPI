package ar.edu.utn.frre.m3.application.usecase;
import ar.edu.utn.frre.m3.domain.SavedCustomerRating;
import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.*;
import java.time.Clock;
import java.util.*;
public class CustomerRatingService {
    private final DriverAccessService access; private final DriverRepository drivers;
    private final CustomerRatingRepository ratings; private final TripContextPort trips;
    private final UnitOfWork transactions; private final Clock clock;
    public CustomerRatingService(DriverAccessService access,DriverRepository drivers,CustomerRatingRepository ratings,
            TripContextPort trips,UnitOfWork transactions,Clock clock) {
        this.access=access; this.drivers=drivers; this.ratings=ratings; this.trips=trips; this.transactions=transactions; this.clock=clock;
    }
    public CustomerRating rate(UUID driverId,String tripId,int score,String comment,String key) {
        access.own(driverId);
        if(tripId==null || tripId.isBlank() || tripId.length()>128 || score<1 || score>5 || (comment!=null && comment.length()>500))
            throw new IllegalArgumentException("Indicá un viaje, una puntuación de 1 a 5 y hasta 500 caracteres de comentario.");
        if(key==null || key.length()<16 || key.length()>128) throw new IllegalArgumentException("Idempotency-Key debe tener entre 16 y 128 caracteres.");
        String fingerprint=tripId+":"+score+":"+(comment==null?"":comment);
        var prior=ratings.findByKey(driverId,key);
        if(prior.isPresent()) return replay(prior.get(),fingerprint);
        // The external request is deliberately outside the database transaction.
        var context=trips.completedTrip(tripId,driverId);
        if(context==null || !tripId.equals(context.tripId()) || !driverId.equals(context.driverId()) || !"COMPLETED".equals(context.status())
                || context.clientId()==null || context.clientId().isBlank() || context.clientId().length()>128)
            throw new DomainValidationException("M6 no confirmó un viaje completado y asignado a este conductor.");
        return transactions.executeInTransaction(() -> {
            drivers.findByIdWithPessimisticLock(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
            var existing=ratings.findByKey(driverId,key);
            if(existing.isPresent()) return replay(existing.get(),fingerprint);
            if(ratings.findByTrip(driverId,tripId).isPresent()) throw new IdempotencyConflictException("Ya valoraste este viaje.");
            CustomerRating rating=new CustomerRating(UUID.randomUUID(),driverId,tripId,context.clientId(),score,comment,clock.instant());
            ratings.save(rating,key,fingerprint); return rating;
        });
    }
    private CustomerRating replay(SavedCustomerRating saved,String fingerprint) {
        if(!saved.fingerprint().equals(fingerprint)) throw new IdempotencyConflictException("La clave ya se utilizó para otra valoración.");
        return saved.rating();
    }
    public List<CustomerRating> history(UUID driverId,int offset,int limit) {
        access.driver(driverId);
        if(offset<0 || limit<1 || limit>100) throw new IllegalArgumentException("Paginación inválida.");
        return ratings.history(driverId,offset,limit);
    }
}
