package ar.edu.utn.frre.m3.hu03;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VehicleRegistrationController {
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
            @RequestParam(required = false) MultipartFile insurance,
            @RequestParam(required = false) MultipartFile registration,
            @RequestParam(required = false) MultipartFile license,
            Model model) {
        if (!type.equals("AUTO") && !type.equals("MOTO")) errors.reject("type", "Seleccioná Auto o Moto.");
        requireFile(insurance, "seguro", errors);
        requireFile(registration, "cédula", errors);
        requireFile(license, "licencia", errors);
        if (errors.hasErrors()) return formPage(type, form, model);

        try {
            long number = storage.save(type, form, insurance, registration, license);
            model.addAttribute("number", number);
            return "hu03/confirmacion";
        } catch (IllegalArgumentException ex) {
            errors.reject("document", ex.getMessage());
            return formPage(type, form, model);
        } catch (IOException ex) {
            errors.reject("storage", "No se pudo guardar el registro. Revisá los permisos de la carpeta del proyecto e intentá nuevamente.");
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

    private void requireFile(MultipartFile file, String name, BindingResult errors) {
        if (file == null || file.isEmpty()) errors.reject("required." + name, "Adjuntá el documento obligatorio: " + name + ".");
    }

    private String formPage(String type, VehicleRegistrationForm form, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("typeLabel", "AUTO".equals(type) ? "Auto" : "Moto");
        model.addAttribute("form", form);
        return "hu03/formulario";
    }
}
