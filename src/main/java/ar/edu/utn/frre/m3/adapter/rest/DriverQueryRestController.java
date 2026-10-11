package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.port.in.GetDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Controlador REST que expone los endpoints de consulta de perfil de conductor (HU-02).
 * - GET /api/v1/me/driver: Conductor autenticado consulta su propio perfil.
 * - GET /api/v1/drivers/{driverId}: Consulta por ID (propietario u operador).
 *
 * Cumple con el contrato OpenAPI 3.0.3 y las normas de arquitectura hexagonal.
 */
@RestController
@RequestMapping("/api/v1")
public class DriverQueryRestController {

    private final GetDriverProfileUseCase getDriverProfileUseCase;
    private final GetMyDriverProfileUseCase getMyDriverProfileUseCase;

    public DriverQueryRestController(GetDriverProfileUseCase getDriverProfileUseCase,
                                     GetMyDriverProfileUseCase getMyDriverProfileUseCase) {
        this.getDriverProfileUseCase = Objects.requireNonNull(getDriverProfileUseCase, "getDriverProfileUseCase es requerido");
        this.getMyDriverProfileUseCase = Objects.requireNonNull(getMyDriverProfileUseCase, "getMyDriverProfileUseCase es requerido");
    }

    /**
     * HU-02: Consultar mi perfil como conductor.
     * Resuelve la identidad del conductor mediante el contexto de seguridad autenticado.
     */
    @GetMapping("/me/driver")
    public ResponseEntity<DriverProfileResponse> getMyDriverProfile() {
        Driver driver = getMyDriverProfileUseCase.getMyDriverProfile();
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, "\"" + driver.getVersion() + "\"")
                .body(DriverProfileResponse.from(driver));
    }

    /**
     * HU-02: Consultar perfil de conductor por identificador único.
     * Permitido al propietario del registro o a un operador administrativo.
     */
    @GetMapping("/drivers/{driverId}")
    public ResponseEntity<DriverProfileResponse> getDriverProfileById(@PathVariable UUID driverId) {
        Driver driver = getDriverProfileUseCase.getDriverProfileById(driverId);
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, "\"" + driver.getVersion() + "\"")
                .body(DriverProfileResponse.from(driver));
    }
}
