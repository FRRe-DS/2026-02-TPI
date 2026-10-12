package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.usecase.CreateDriverProfileUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Controlador REST para operaciones sobre conductores.
 * Expone POST /api/v1/drivers para la creación de perfiles (HU-01 / CLON-6).
 */
@RestController
@RequestMapping("/api/v1/drivers")
public class DriverRestController {

    private final CreateDriverProfileUseCase createDriverProfileUseCase;

    public DriverRestController(CreateDriverProfileUseCase createDriverProfileUseCase) {
        this.createDriverProfileUseCase = createDriverProfileUseCase;
    }

    @PostMapping
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody CreateDriverRequest request, org.springframework.security.core.Authentication authentication) {
        if(authentication!=null && authentication.isAuthenticated() && !authentication.getName().equals(request.getIdentitySubject()))
            throw new ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException("La identidad del perfil debe coincidir con el token.");
        Driver createdDriver = createDriverProfileUseCase.execute(
                request.getIdentitySubject(),
                request.getName()
        );

        URI location = URI.create("/api/v1/drivers/" + createdDriver.getId());
        return ResponseEntity.created(location).eTag("\""+createdDriver.getVersion()+"\"").body(DriverResponse.fromDomain(createdDriver));
    }
}
