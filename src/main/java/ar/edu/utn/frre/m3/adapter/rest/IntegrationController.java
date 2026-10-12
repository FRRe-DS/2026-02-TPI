package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.BatchEligibilityRequestDTO;
import ar.edu.utn.frre.m3.adapter.rest.dto.BatchEligibilityResponseDTO;
import ar.edu.utn.frre.m3.adapter.rest.dto.EligibilityResponseDTO;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integration")
public class IntegrationController {

    private final CheckDriverAptitudeUseCase checkDriverAptitudeUseCase;
    private final ar.edu.utn.frre.m3.application.port.DriverRepository drivers;

    public IntegrationController(CheckDriverAptitudeUseCase checkDriverAptitudeUseCase, ar.edu.utn.frre.m3.application.port.DriverRepository drivers) {
        this.drivers=drivers;
        this.checkDriverAptitudeUseCase = Objects.requireNonNull(checkDriverAptitudeUseCase);
    }

    @GetMapping("/drivers/{driverId}/eligibility")
    public ResponseEntity<EligibilityResponseDTO> getEligibility(@PathVariable UUID driverId) {
        EligibilityResult result = checkDriverAptitudeUseCase.execute(driverId);
        return ResponseEntity.ok(EligibilityResponseDTO.from(result));
    }

    @PostMapping("/eligibility/batch")
    public ResponseEntity<BatchEligibilityResponseDTO> batchEligibility(@Valid @RequestBody BatchEligibilityRequestDTO request) {
        List<BatchEligibilityResponseDTO.DriverEligibilityEntry> results = new ArrayList<>();
        for (UUID id : request.driverIds()) {
            try {
                EligibilityResult res = checkDriverAptitudeUseCase.execute(id);
                results.add(new BatchEligibilityResponseDTO.DriverEligibilityEntry(id, EligibilityResponseDTO.from(res)));
            } catch (DriverNotFoundException e) {
                results.add(new BatchEligibilityResponseDTO.DriverEligibilityEntry(
                    id,
                    new EligibilityResponseDTO(false, List.of("DRIVER_NOT_FOUND"), Instant.now(), 0, Instant.now())
                ));
            }
        }
        return ResponseEntity.ok(new BatchEligibilityResponseDTO(results));
    }

    @GetMapping("/drivers/{driverId}/summary")
    public ResponseEntity<DriverSummaryDTO> getSummary(@PathVariable UUID driverId) {
        EligibilityResult result = checkDriverAptitudeUseCase.execute(driverId);
        return ResponseEntity.ok(new DriverSummaryDTO(driverId, drivers.findById(driverId).orElseThrow(() -> DriverNotFoundException.forId(driverId)).getDisplayName(), result.eligible()));
    }

    public record DriverSummaryDTO(UUID id, String displayName, boolean eligible) {}
}
