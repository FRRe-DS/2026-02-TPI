package ar.edu.utn.frre.m3.hu03;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class VehicleRegistrationController {
    private static final Set<String> ALLOWED_SERVICES = Set.of("STANDARD", "PREMIUM", "DELIVERY", "PETS");

    private final VehicleCsvStorage storage;
    private final ConfigurableApplicationContext applicationContext;

    public VehicleRegistrationController(VehicleCsvStorage storage, ConfigurableApplicationContext applicationContext) {
        this.storage = storage;
        this.applicationContext = applicationContext;
    }

    @GetMapping("/")
    public String start() { return "hu03/inicio"; }

    @PostMapping("/registro/comenzar")
    public String begin() { return "redirect:/registro/seleccion"; }

    @GetMapping("/registro/seleccion")
    public String selection() { return "hu03/seleccion"; }

    @PostMapping("/registro/seleccion")
    public String choose(@RequestParam String type, Model model) {
        if (!type.equals("AUTO") && !type.equals("MOTO")) {
            model.addAttribute("error", "Seleccioná Auto o Moto para continuar.");
            return "hu03/seleccion";
        }
        model.addAttribute("type", type);
        model.addAttribute("typeLabel", type.equals("AUTO") ? "Auto" : "Moto");
        model.addAttribute("form", new VehicleRegistrationForm());
        return "hu03/formulario";
    }

    @PostMapping("/registro/guardar")
    public String save(@RequestParam String type,
            @Valid @ModelAttribute("form") VehicleRegistrationForm form,
            BindingResult errors,
            Model model) {
        if (!type.equals("AUTO") && !type.equals("MOTO")) errors.reject("type", "Seleccioná Auto o Moto.");
        if (form.getServices() == null || !ALLOWED_SERVICES.containsAll(form.getServices())) {
            errors.rejectValue("services", "services.invalid", "Seleccioná uno o más tipos de servicio válidos.");
        }
        if (errors.hasErrors()) return formPage(type, form, model);

        try {
            long number = storage.save(type, form);
            model.addAttribute("number", number);
            return "hu03/confirmacion";
        } catch (IOException ex) {
            errors.reject("storage", "No se pudo guardar el registro. Revisá la carpeta vehiculos e intentá nuevamente.");
            return formPage(type, form, model);
        }
    }

    @GetMapping("/registro/otro")
    public String another() { return "redirect:/registro/seleccion"; }

    @PostMapping("/registro/finalizar")
    public String finish() {
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS)
                .execute(applicationContext::close);
        return "hu03/finalizada";
    }

    private String formPage(String type, VehicleRegistrationForm form, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("typeLabel", "AUTO".equals(type) ? "Auto" : "Moto");
        model.addAttribute("form", form);
        return "hu03/formulario";
    }
}