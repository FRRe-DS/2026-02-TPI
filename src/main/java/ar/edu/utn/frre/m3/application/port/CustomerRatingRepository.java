package ar.edu.utn.frre.m3.application.port;
import ar.edu.utn.frre.m3.domain.SavedCustomerRating;
import ar.edu.utn.frre.m3.domain.CustomerRating;
import java.util.*;
public interface CustomerRatingRepository {
    Optional<SavedCustomerRating> findByKey(UUID driverId,String key);
    Optional<CustomerRating> findByTrip(UUID driverId,String tripId);
    void save(CustomerRating rating,String key,String fingerprint);
    List<CustomerRating> history(UUID driverId,int offset,int limit);
}
