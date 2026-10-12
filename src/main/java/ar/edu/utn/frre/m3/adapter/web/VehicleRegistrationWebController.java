package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.application.usecase.DriverWorkspaceService;
import ar.edu.utn.frre.m3.domain.exception.VersionMismatchException;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** HU-03's frontend, adapted to M3's shared application and persistence. */
@Controller
@RequestMapping("/ui/vehicles/register")
public class VehicleRegistrationWebController {
    private final DriverWorkspaceService workspace;
    public VehicleRegistrationWebController(DriverWorkspaceService workspace) { this.workspace=workspace; }
    @GetMapping public String select() { workspace.current(); return "hu03/seleccion"; }
    @PostMapping("/type") public String type(@RequestParam String type, Model model) {
        if(!Set.of("AUTO","MOTO").contains(type)) {
            model.addAttribute("error","Seleccioná Auto o Moto."); return "hu03/seleccion";
        }
        VehicleRegistrationForm form=new VehicleRegistrationForm(); form.setVersion(workspace.current().getVersion());
        model.addAttribute("form",form); return form(type,model);
    }
    @PostMapping public String save(@RequestParam String type, @Valid @ModelAttribute("form") VehicleRegistrationForm form,
            BindingResult errors, Model model, RedirectAttributes flash) {
        if(!errors.hasErrors()) {
            try {
                workspace.registerVehicle(type,form.getPlate(),form.getBrand(),form.getModel(),form.getYear(),form.getVersion(),
                    form.getColor(),form.getCapacity(),Set.copyOf(form.getServices()));
                flash.addFlashAttribute("success","Vehículo registrado en M3. Quedó pendiente de revisión.");
                return "redirect:/ui/vehicles";
            } catch(IllegalArgumentException ex) { errors.reject("vehicle",ex.getMessage()); }
              catch(VersionMismatchException ex) { errors.reject("version","Los datos cambiaron. Volvé a abrir el registro antes de enviar."); }
        }
        return form(type,model);
    }
    private String form(String type,Model model) {
        model.addAttribute("type",type); model.addAttribute("typeLabel","AUTO".equals(type)?"Auto":"Moto");
        return "hu03/formulario";
    }
}
