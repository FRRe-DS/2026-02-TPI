package ar.edu.utn.frre.m3.adapter.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Navigable UI preview while the remaining application use cases and M1 roles are integrated.
 * Example data is deliberately kept out of persistence and is identified in the view.
 */
@Controller
public class UiPreviewController {
    private static final List<Map<String, String>> DRIVERS = List.of(
            Map.of("id", "9d27b903-62de-4b9a-8855-91b63d8c7f21", "name", "María Fernández", "status", "Aprobado", "service", "Auto y moto", "updated", "10 oct 2026"),
            Map.of("id", "bd3e38eb-c044-4918-a159-3ee957b0df00", "name", "Julián Torres", "status", "Pendiente", "service", "Auto", "updated", "09 oct 2026"),
            Map.of("id", "2c7464cc-0c10-4e12-89c8-c91cdbcb4915", "name", "Lucía Gómez", "status", "Observado", "service", "Moto", "updated", "08 oct 2026")
    );

    @GetMapping({"/ui/preview", "/ui/preview/"})
    public String home(Model model) { return page(model, "home", "Inicio"); }

    @GetMapping("/ui/preview/profile")
    public String profile(Model model) { return page(model, "profile", "Mi perfil"); }

    @GetMapping("/ui/vehicles")
    public String vehicles(Model model) { return page(model, "vehicles", "Vehículos"); }

    @GetMapping("/ui/documents")
    public String documents(Model model) { return page(model, "documents", "Documentación"); }

    @GetMapping("/ui/qualification")
    public String qualification(Model model) { return page(model, "qualification", "Habilitación"); }

    @GetMapping("/ui/availability")
    public String availability(Model model) { return page(model, "availability", "Disponibilidad"); }

    @GetMapping("/ui/customer-ratings")
    public String ratings(Model model) { return page(model, "ratings", "Valoraciones"); }

    @GetMapping("/ui/operator/drivers")
    public String operatorDrivers(Model model) {
        model.addAttribute("drivers", DRIVERS);
        return page(model, "operator", "Conductores");
    }

    @GetMapping("/ui/operator/drivers/{driverId}")
    public String operatorDetail(@PathVariable UUID driverId, Model model) {
        model.addAttribute("selectedDriver", DRIVERS.stream()
                .filter(driver -> driver.get("id").equals(driverId.toString()))
                .findFirst().orElse(Map.of("id", driverId.toString(), "name", "Conductor", "status", "Pendiente", "service", "—", "updated", "—")));
        return page(model, "operator-detail", "Detalle del conductor");
    }

    private String page(Model model, String page, String title) {
        model.addAttribute("page", page);
        model.addAttribute("title", title);
        return "ui";
    }
}
