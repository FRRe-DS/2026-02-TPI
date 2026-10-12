package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.application.usecase.*;
import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkspaceWebController {
    private final DriverWorkspaceService workspace;
    private final RequestAvailabilityUseCase availability;
    private final RegisterVehicleDocumentUseCase vehicleDocuments;
    private final ReviewDocumentUseCase reviews;
    private final CreateDriverProfileUseCase createDriver;
    public WorkspaceWebController(DriverWorkspaceService workspace, RequestAvailabilityUseCase availability,
            RegisterVehicleDocumentUseCase vehicleDocuments, ReviewDocumentUseCase reviews, CreateDriverProfileUseCase createDriver) {
        this.workspace=workspace; this.availability=availability; this.vehicleDocuments=vehicleDocuments;
        this.reviews=reviews; this.createDriver=createDriver;
    }
    @GetMapping("/") public String root() { return "redirect:/ui"; }
    @GetMapping("/ui/register") public String registration(Model model) { return page(model,"register","Crear mi perfil"); }
    @PostMapping("/ui/register") public String register(@RequestParam String name, RedirectAttributes flash) {
        if(name.isBlank() || name.length()>100) throw new IllegalArgumentException("Ingresá un nombre de hasta 100 caracteres.");
        createDriver.execute(workspace.actor(),name);
        flash.addFlashAttribute("success","Perfil registrado. Quedó pendiente de revisión.");
        return "redirect:/ui/profile";
    }
    @GetMapping("/ui/vehicles") public String vehicles(Model model) {
        Driver driver=workspace.current(); model.addAttribute("driver",driver);
        model.addAttribute("vehicles",workspace.vehicles(driver.getId()));
        return page(model,"vehicles","Mis vehículos");
    }
    @PostMapping("/ui/vehicles") public String registerVehicle(@RequestParam String type, @RequestParam String plate,
            @RequestParam String make, @RequestParam String model, @RequestParam Integer year,
            @RequestParam long version, RedirectAttributes flash) {
        try { workspace.registerVehicle(type,plate,make,model,year,version);
            flash.addFlashAttribute("success","Vehículo registrado. Quedó pendiente de revisión.");
        } catch(IllegalArgumentException | VersionMismatchException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/ui/vehicles";
    }
    @GetMapping("/ui/documents") public String documents(Model model) {
        Driver driver=workspace.current(); model.addAttribute("driver",driver);
        model.addAttribute("vehicles",workspace.vehicles(driver.getId()));
        model.addAttribute("documents",workspace.documents(driver.getId()));
        return page(model,"documents","Mi documentación");
    }
    @PostMapping("/ui/documents/vehicle") public String document(@RequestParam UUID vehicleId,
            @RequestParam DocumentType documentType, @RequestParam String documentNumber,
            @RequestParam String issuingEntity, @RequestParam LocalDate issuedAt, @RequestParam LocalDate validUntil,
            RedirectAttributes flash) {
        try {
            if(documentType==DocumentType.LICENSE) throw new IllegalArgumentException("Usá el formulario de licencia del conductor.");
            if(documentNumber.isBlank() || documentNumber.length()>80 || issuingEntity.length()>100)
                throw new IllegalArgumentException("Revisá el número de documento y la entidad emisora.");
            ZoneId zone=ZoneId.of("America/Argentina/Buenos_Aires");
            vehicleDocuments.registerVehicleDocument(new RegisterVehicleDocumentCommand(workspace.current().getId(),vehicleId,
                    documentType,documentNumber,issuingEntity,issuedAt.atStartOfDay(zone).toInstant(),
                    validUntil.plusDays(1).atStartOfDay(zone).toInstant(),null));
            flash.addFlashAttribute("success","Documento registrado y pendiente de revisión.");
        } catch(IllegalArgumentException | DomainValidationException | VehicleForbiddenException | VehicleNotFoundException ex) {
            flash.addFlashAttribute("error",ex.getMessage());
        }
        return "redirect:/ui/documents";
    }
    @GetMapping("/ui/availability") public String availability(Model model) {
        Driver driver=workspace.current(); model.addAttribute("driver",driver);
        model.addAttribute("vehicles",workspace.vehicles(driver.getId()));
        model.addAttribute("intent",workspace.availability(driver.getId()));
        model.addAttribute("idempotencyKey",UUID.randomUUID().toString());
        return page(model,"availability","Disponibilidad");
    }
    @PostMapping("/ui/availability") public String availability(@RequestParam UUID vehicleId,
            @RequestParam(defaultValue="false") boolean connected, @RequestParam(defaultValue="false") boolean available,
            @RequestParam String idempotencyKey, RedirectAttributes flash) throws java.security.NoSuchAlgorithmException {
        try {
            if(idempotencyKey.length()<16 || idempotencyKey.length()>128) throw new IllegalArgumentException("Recargá el formulario antes de enviarlo.");
            UUID driverId=workspace.current().getId();
            String body=driverId+":"+vehicleId+":"+connected+":"+available;
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
            availability.execute(new RequestAvailabilityCommand(driverId,vehicleId,connected,available,idempotencyKey,hash,
                    workspace.actor(),UUID.randomUUID().toString()));
            flash.addFlashAttribute("success","Solicitud guardada. La confirmación de M4 sigue pendiente.");
        } catch(IllegalArgumentException | DriverNotQualifiedException | IdempotencyConflictException ex) {
            flash.addFlashAttribute("error",ex.getMessage());
        }
        return "redirect:/ui/availability";
    }
    @GetMapping("/ui/operator/drivers") public String operators(Model model) {
        model.addAttribute("drivers",workspace.allDrivers()); return page(model,"operator","Conductores");
    }
    @GetMapping("/ui/operator/drivers/{id}") public String detail(@PathVariable UUID id,Model model) {
        workspace.requireOperator(); model.addAttribute("driver",workspace.driver(id));
        model.addAttribute("vehicles",workspace.vehicles(id)); model.addAttribute("documents",workspace.documents(id));
        return page(model,"operator-detail","Revisión del conductor");
    }
    @PostMapping("/ui/operator/drivers/{id}/documents/{documentId}/review")
    public String review(@PathVariable UUID id,@PathVariable UUID documentId,@RequestParam ReviewDecision decision,
            @RequestParam(defaultValue="") String reason,@RequestParam long version,RedirectAttributes flash) {
        workspace.requireOperator(); workspace.driver(id);
        try {
            reviews.reviewDocument(new ReviewDocumentCommand(id,documentId,decision,reason.isBlank() ? null : reason,version,workspace.actor(),UUID.randomUUID().toString()));
            flash.addFlashAttribute("success","Revisión guardada.");
        } catch(IllegalArgumentException | DomainValidationException | VersionMismatchException ex) {
            flash.addFlashAttribute("error",ex.getMessage());
        }
        return "redirect:/ui/operator/drivers/"+id;
    }
    @GetMapping("/ui/customer-ratings") public String ratings(Model model) { return page(model,"ratings","Valoraciones"); }
    @ExceptionHandler({IllegalArgumentException.class, DriverAccessDeniedException.class})
    public String error(RuntimeException ex,Model model) {
        model.addAttribute("error",ex.getMessage()); return page(model,"error","No se pudo completar la operación");
    }
    private String page(Model model,String page,String title) { model.addAttribute("page",page); model.addAttribute("title",title); return "workspace"; }
}
