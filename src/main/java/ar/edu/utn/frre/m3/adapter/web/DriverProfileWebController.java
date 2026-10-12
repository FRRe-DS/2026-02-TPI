package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.UUID;

/**
 * Controlador Web (Thymeleaf) para la interfaz de usuario de M3.
 * Atiende las vistas de consulta de perfil de conductor (HU-02):
 * - /ui: Inicio de la interfaz de conductor
 * - /ui/profile: Vista detallada del perfil y estado actual
 * - /ui/profile?driverId={id}: Vista con parámetro de consulta (para pruebas u operador)
 */
@Controller
@RequestMapping("/ui")
public class DriverProfileWebController {

    private final GetMyDriverProfileUseCase getMyDriverProfileUseCase;
    private final GetDriverProfileUseCase getDriverProfileUseCase;

    public DriverProfileWebController(GetMyDriverProfileUseCase getMyDriverProfileUseCase,
                                      GetDriverProfileUseCase getDriverProfileUseCase) {
        this.getMyDriverProfileUseCase = Objects.requireNonNull(getMyDriverProfileUseCase, "getMyDriverProfileUseCase es requerido");
        this.getDriverProfileUseCase = Objects.requireNonNull(getDriverProfileUseCase, "getDriverProfileUseCase es requerido");
    }

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/ui/profile";
    }

    @GetMapping("/profile")
    public String viewProfile(@RequestParam(name = "driverId", required = false) String driverIdParam,
                              @RequestParam(name = "switchSession", defaultValue = "false") boolean switchSession,
                              HttpServletRequest request,
                              Model model) {
        try {
            Driver driver;
            if (driverIdParam != null && !driverIdParam.isBlank()) {
                driver = getDriverProfileUseCase.getDriverProfileById(UUID.fromString(driverIdParam));
                if (switchSession && driver != null && request != null && Boolean.TRUE.equals(request.getAttribute("m3.development"))) {
                    request.getSession().setAttribute("SIMULATED_SUBJECT", driver.getIdentitySubject());
                }
            } else {
                driver = getMyDriverProfileUseCase.getMyDriverProfile();
            }

            model.addAttribute("driver", DriverProfileViewModel.from(driver));
            return "profile";
        } catch (DriverNotFoundException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("errorType", "NOT_FOUND");
            return "error";
        } catch (DriverAccessDeniedException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("errorType", "ACCESS_DENIED");
            return "error";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", "Identificador de conductor con formato inválido.");
            model.addAttribute("errorType", "BAD_REQUEST");
            return "error";
        }
    }

    @GetMapping("/profile/{driverId}")
    public String viewProfileByPath(@PathVariable UUID driverId, HttpServletRequest request, Model model) {
        return viewProfile(driverId.toString(), false, request, model);
    }
}
