package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.application.usecase.*;
import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.adapter.security.DevelopmentMode;
import ar.edu.utn.frre.m3.adapter.security.DemoAccessAuthenticationFilter;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WorkspaceWebController {
    private final QualificationService qualification;
    private final CustomerRatingService ratings;
    private final ar.edu.utn.frre.m3.application.port.AvailabilityDeliveryPort delivery;
    private final DriverWorkspaceService workspace;
    private final RequestAvailabilityUseCase availability;
    private final RegisterVehicleDocumentUseCase vehicleDocuments;
    private final ReviewDocumentUseCase reviews;
    private final CreateDriverProfileUseCase createDriver;
    private final DevelopmentMode developmentMode;
    @Value("${app.demo-access.enabled:false}") private boolean demoAccessEnabled;
    @Value("${app.demo-access.subject:}") private String demoAccessSubject;
    public WorkspaceWebController(DriverWorkspaceService workspace, RequestAvailabilityUseCase availability,
            RegisterVehicleDocumentUseCase vehicleDocuments, ReviewDocumentUseCase reviews, CreateDriverProfileUseCase createDriver, QualificationService qualification, CustomerRatingService ratings, ar.edu.utn.frre.m3.application.port.AvailabilityDeliveryPort delivery, DevelopmentMode developmentMode) {
        this.qualification=qualification; this.ratings=ratings; this.delivery=delivery;
        this.workspace=workspace; this.availability=availability; this.vehicleDocuments=vehicleDocuments;
        this.reviews=reviews; this.createDriver=createDriver; this.developmentMode=developmentMode;
    }
    @GetMapping("/") public String root(Model model) { return access(model,false); }
    @GetMapping("/login/m1") public String m1Unavailable(Model model) { return access(model,true); }
    @GetMapping("/demo/access") public String demoAccess(jakarta.servlet.http.HttpServletRequest request) {
        if(!demoAccessEnabled || demoAccessSubject.isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        request.getSession(true).setAttribute(DemoAccessAuthenticationFilter.SUBJECT_ATTRIBUTE,demoAccessSubject);
        return "redirect:/ui/profile";
    }
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
        } catch(IllegalArgumentException | DomainValidationException | DriverNotQualifiedException | IdempotencyConflictException ex) {
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
        var decisions=new ArrayList<>(qualification.history(id,null,0,100));
        for(var v:workspace.vehicles(id)) decisions.addAll(qualification.history(id,v.getId(),0,100));
        decisions.sort(java.util.Comparator.comparing(QualificationDecision::occurredAt).reversed());
        model.addAttribute("decisions",decisions);
        model.addAttribute("idempotencyKey",UUID.randomUUID().toString());
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
    @PostMapping("/ui/operator/drivers/{id}/qualification")
    public String qualify(@PathVariable UUID id,@RequestParam(required=false) UUID vehicleId,
            @RequestParam QualificationStatus status,@RequestParam String reason,@RequestParam long version,
            @RequestParam String idempotencyKey,RedirectAttributes flash) {
        try { qualification.decide(id,vehicleId,status,reason,version,idempotencyKey); flash.addFlashAttribute("success","Decisión guardada con su historial."); }
        catch(DomainValidationException | IllegalArgumentException | VersionMismatchException | IdempotencyConflictException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/ui/operator/drivers/"+id;
    }
    @PostMapping("/ui/availability/retry")
    public String retry(RedirectAttributes flash) {
        delivery.retry(workspace.current().getId(),java.time.Instant.now());
        flash.addFlashAttribute("success","Se programó el reintento de la última solicitud fallida.");
        return "redirect:/ui/availability";
    }
    @GetMapping("/ui/customer-ratings") public String ratings(Model model) {
        Driver driver=workspace.current(); model.addAttribute("ratings",ratings.history(driver.getId(),0,100));
        model.addAttribute("idempotencyKey",UUID.randomUUID().toString());
        return page(model,"ratings","Valoraciones");
    }
    @PostMapping("/ui/customer-ratings") public String rate(@RequestParam String tripId,@RequestParam int score,
            @RequestParam(defaultValue="") String comment,@RequestParam String idempotencyKey,RedirectAttributes flash) {
        try { ratings.rate(workspace.current().getId(),tripId,score,comment,idempotencyKey); flash.addFlashAttribute("success","Valoración registrada."); }
        catch(DomainValidationException | IllegalArgumentException | IntegrationUnavailableException | IdempotencyConflictException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/ui/customer-ratings";
    }
    @ExceptionHandler({IllegalArgumentException.class, DriverAccessDeniedException.class, ar.edu.utn.frre.m3.application.exception.DriverAlreadyExistsException.class})
    public String error(RuntimeException ex,Model model) {
        model.addAttribute("error",ex.getMessage()); return page(model,"error","No se pudo completar la operación");
    }
    private String page(Model model,String page,String title) { model.addAttribute("page",page); model.addAttribute("title",title); return "workspace"; }
    private String access(Model model,boolean m1Unavailable) {
        model.addAttribute("development",developmentMode.enabled());
        model.addAttribute("m1Unavailable",m1Unavailable && developmentMode.enabled());
        model.addAttribute("demoAccessAvailable",demoAccessEnabled && !demoAccessSubject.isBlank());
        return "access";
    }
}
