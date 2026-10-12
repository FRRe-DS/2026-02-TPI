package ar.edu.utn.frre.m3.adapter.rest;
import ar.edu.utn.frre.m3.application.usecase.QualificationService;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.PreconditionRequiredException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/drivers/{driverId}")
public class QualificationController {
    private final QualificationService service;
    public QualificationController(QualificationService service) { this.service=service; }
    public record Decision(QualificationStatus status,String reason) {}
    @PostMapping({"/qualification","/vehicles/{vehicleId}/qualification"})
    public ResponseEntity<QualificationDecision> decide(@PathVariable UUID driverId,
            @PathVariable(required=false) UUID vehicleId,@RequestBody Decision body,
            @RequestHeader(value="If-Match",required=false) String version,@RequestHeader("Idempotency-Key") String key) {
        if(version==null) throw new PreconditionRequiredException("If-Match es obligatorio.");
        if(!version.matches("\"[1-9][0-9]*\"")) throw new IllegalArgumentException("If-Match debe ser una versión entre comillas.");
        var result=service.decide(driverId,vehicleId,body.status(),body.reason(),Long.parseLong(version.substring(1,version.length()-1)),key);
        return ResponseEntity.ok().eTag(String.valueOf(result.driverVersion())).body(result);
    }
    @GetMapping({"/qualification/history","/vehicles/{vehicleId}/qualification/history"})
    public List<QualificationDecision> history(@PathVariable UUID driverId,@PathVariable(required=false) UUID vehicleId,
            @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="25") int limit) {
        return service.history(driverId,vehicleId,offset,limit);
    }
}
