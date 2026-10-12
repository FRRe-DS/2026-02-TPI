package ar.edu.utn.frre.m3.adapter.rest;
import ar.edu.utn.frre.m3.application.usecase.CustomerRatingService;
import ar.edu.utn.frre.m3.domain.CustomerRating;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/drivers/{driverId}/customer-ratings")
public class CustomerRatingController {
    private final CustomerRatingService service;
    public CustomerRatingController(CustomerRatingService service) { this.service=service; }
    public record Rating(String tripId,int score,String comment) {}
    @PostMapping public CustomerRating rate(@PathVariable UUID driverId,@RequestBody Rating body,@RequestHeader("Idempotency-Key") String key) {
        return service.rate(driverId,body.tripId(),body.score(),body.comment(),key);
    }
    @GetMapping public List<CustomerRating> history(@PathVariable UUID driverId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="25") int limit) {
        return service.history(driverId,offset,limit);
    }
}
