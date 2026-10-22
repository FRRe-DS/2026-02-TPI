package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.adapter.persistence.entity.*;
import ar.edu.utn.frre.m3.adapter.persistence.repository.*;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceFlowTest {
    @Autowired MockMvc mvc;
    @Autowired SpringDataDriverRepository drivers;
    @Autowired SpringDataVehicleRepository vehicles;
    @Autowired DocumentRepository documents;
    @Autowired SpringDataAvailabilityIntentRepository intents;
    @Autowired SpringDataOutboxRepository outbox;
    UUID driverId;
    String subject;
    @BeforeEach void seed() {
        driverId=UUID.randomUUID(); subject="workspace-"+driverId;
        drivers.saveAndFlush(new DriverJpaEntity(driverId,subject,"Prueba Integrada","APPROVED",1L,Instant.now(),Instant.now()));
    }
    @Test void publicEntryStartsTheM1LoginFlow() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk()).andExpect(view().name("access"))
            .andExpect(content().string(containsString("Ingresar con M1")))
            .andExpect(content().string(containsString("/login/m1")));
        mvc.perform(get("/login/m1"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("M1 no está iniciado")));
    }
    @Test void rendersEveryIntegratedScreen() throws Exception {
        for(String path : new String[]{"/ui/profile","/ui/vehicles","/ui/documents","/ui/availability","/ui/qualification",
                "/ui/operator/drivers","/ui/operator/drivers/"+driverId,"/ui/customer-ratings","/ui/register","/ui/drivers/"+driverId+"/documents"}) {
            mvc.perform(get(path).with(user(subject).roles("DRIVER","OPERATOR")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"));
        }
    }
    @Test void navigationShowsOnlyTheAreasAllowedByRoles() throws Exception {
        mvc.perform(get("/ui/vehicles").with(user(subject).roles("DRIVER")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("MI ESPACIO")))
            .andExpect(content().string(containsString("Mi perfil")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("GESTIÓN"))));

        mvc.perform(get("/ui/operator/drivers").with(user("operator-only").roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("GESTIÓN")))
            .andExpect(content().string(containsString("Conductores")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("MI ESPACIO"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Mi perfil"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Crear perfil"))));

        mvc.perform(get("/ui/operator/drivers").with(user(subject).roles("DRIVER","OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("MI ESPACIO")))
            .andExpect(content().string(containsString("GESTIÓN")));
    }
    @Test void adminUsesTheOperatorLandingPage() throws Exception {
        mvc.perform(get("/ui").with(user("admin-only").roles("ADMIN")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/ui/operator/drivers"));
    }
    @Test void vehicleDocumentReviewAndAvailabilityPersistThroughWebForms() throws Exception {
        mvc.perform(post("/ui/vehicles").with(user(subject).roles("DRIVER")).with(csrf())
                .param("type","AUTO").param("plate","ab123cd").param("make","Toyota").param("model","Corolla")
                .param("year","2022").param("version","1"))
            .andExpect(redirectedUrl("/ui/vehicles")).andExpect(flash().attributeExists("success"));
        var registered=vehicles.findByDriverId(driverId);
        assertEquals(1,registered.size()); var vehicle=registered.getFirst();
        assertEquals("AB123CD",vehicle.getPlate()); assertEquals("PENDING",vehicle.getQualificationStatus());
        assertEquals(2L,drivers.findById(driverId).orElseThrow().getVersion());
        mvc.perform(get("/ui/vehicles").with(user(subject))).andExpect(content().string(containsString("AB123CD")));
        mvc.perform(post("/ui/vehicles").with(user(subject)).with(csrf()).param("type","AUTO").param("plate","ZZ999ZZ")
                .param("make","Toyota").param("model","Corolla").param("year","2022").param("version","1"))
            .andExpect(flash().attributeExists("error"));
        assertEquals(1,vehicles.findByDriverId(driverId).size());
        mvc.perform(post("/ui/documents/vehicle").with(user(subject)).with(csrf())
                .param("vehicleId",vehicle.getId().toString()).param("documentType","INSURANCE")
                .param("documentNumber","POL-123").param("issuingEntity","Aseguradora")
                .param("issuedAt","2026-01-01").param("validUntil","2027-12-31"))
            .andExpect(redirectedUrl("/ui/documents")).andExpect(flash().attributeExists("success"));
        var doc=documents.findByVehicleIdOrderByRevisionDesc(vehicle.getId()).getFirst();
        assertEquals(DocumentStatus.PENDING,doc.getStatus());
        mvc.perform(get("/ui/documents").with(user(subject))).andExpect(status().isOk()).andExpect(content().string(containsString("INSURANCE")));
        mvc.perform(get("/ui/operator/drivers/"+driverId).with(user(subject).roles("OPERATOR")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Documentación del conductor")))
            .andExpect(content().string(containsString("Documentación de vehículos")))
            .andExpect(content().string(containsString("Seguro")))
            .andExpect(content().string(containsString("Toyota Corolla · AB123CD")))
            .andExpect(content().string(containsString("Guardar revisión")));
        long version=drivers.findById(driverId).orElseThrow().getVersion();
        mvc.perform(post("/ui/operator/drivers/"+driverId+"/documents/"+doc.getId()+"/review")
                .with(user(subject).roles("OPERATOR")).with(csrf()).param("decision","APPROVED").param("reason","Documentación verificada").param("version",Long.toString(version)))
            .andExpect(flash().attributeExists("success"));
        assertEquals(DocumentStatus.APPROVED,documents.findById(doc.getId()).orElseThrow().getStatus());
        String key=UUID.randomUUID().toString();
        mvc.perform(post("/ui/availability").with(user(subject)).with(csrf()).param("vehicleId",vehicle.getId().toString())
                .param("connected","true").param("available","true").param("idempotencyKey",key))
            .andExpect(flash().attributeExists("error"));
        assertTrue(intents.findByDriverId(driverId).isEmpty());
        // Operator qualification is a separate use case: seed its result to test HU-10's approved path.
        vehicle.setQualificationStatus("APPROVED"); vehicles.saveAndFlush(vehicle);
        var now=Instant.now();
        var license=ar.edu.utn.frre.m3.domain.Document.createPendingLicense(driverId,"TEST-LICENSE",now.minusSeconds(100),now.plusSeconds(86400),java.util.Set.of(ar.edu.utn.frre.m3.domain.ServiceType.AUTO),null,1,version);
        license.review(ar.edu.utn.frre.m3.domain.ReviewDecision.APPROVED,"Verificado","test",version); documents.save(license);
        var registration=ar.edu.utn.frre.m3.domain.Document.createPendingRevision(driverId,vehicle.getId(),ar.edu.utn.frre.m3.domain.DocumentType.VEHICLE_REGISTRATION,"REG","Test",now.minusSeconds(100),now.plusSeconds(86400),null,1);
        registration.review(ar.edu.utn.frre.m3.domain.ReviewDecision.APPROVED,"Verificado","test",version); documents.save(registration);
        long messages=outbox.count();
        for(int i=0;i<2;i++) mvc.perform(post("/ui/availability").with(user(subject)).with(csrf())
                .param("vehicleId",vehicle.getId().toString()).param("connected","true").param("available","true").param("idempotencyKey",key))
            .andExpect(redirectedUrl("/ui/availability")).andExpect(flash().attributeExists("success"));
        assertTrue(intents.findByDriverId(driverId).orElseThrow().isAvailable());
        assertEquals(messages+1,outbox.count());
        mvc.perform(get("/ui/availability").with(user(subject))).andExpect(status().isOk()).andExpect(content().string(containsString("PENDING")));
    }
    @Test void integratedHu03PreservesEveryFormField() throws Exception {
        mvc.perform(get("/ui/vehicles/register").with(user(subject))).andExpect(status().isOk());
        mvc.perform(post("/ui/vehicles/register/type").with(user(subject)).with(csrf()).param("type","AUTO"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Registrar vehículo")));
        mvc.perform(post("/ui/vehicles/register").with(user(subject)).with(csrf()).param("type","AUTO")
                .param("plate","CD456EF").param("brand","Ford").param("model","Focus").param("year","2020")
                .param("color","Azul").param("capacity","4").param("services","STANDARD","PETS").param("version","1"))
            .andExpect(redirectedUrl("/ui/vehicles")).andExpect(flash().attributeExists("success"));
        var v=vehicles.findByDriverId(driverId).getFirst();
        assertEquals("Azul",v.getColor()); assertEquals(4,v.getCapacity());
        assertEquals("PETS,STANDARD",v.getServiceCategories()); assertEquals("AUTO",v.getServiceTypes());
        mvc.perform(get("/ui/vehicles").with(user(subject))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Azul"))).andExpect(content().string(containsString("PETS")));
        mvc.perform(post("/ui/vehicles/register").with(user(subject)).with(csrf()).param("type","AUTO")
                .param("plate","INVALID").param("brand","Ford").param("model","Focus").param("year","2020")
                .param("color","Azul").param("capacity","8").param("services","STANDARD").param("version","2"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","capacity"));
        assertEquals(1,vehicles.findByDriverId(driverId).size());
    }
    @Test void csrfAndOperatorRoleAreEnforced() throws Exception {
        mvc.perform(post("/ui/vehicles").with(user(subject))).andExpect(status().isForbidden());
        mvc.perform(get("/ui/operator/drivers").with(user(subject).roles("DRIVER")))
            .andExpect(status().isForbidden());
    }
    @Test void profileCanBeCreatedFromCurrentSession() throws Exception {
        String newSubject="new-"+UUID.randomUUID();
        mvc.perform(post("/ui/register").with(user(newSubject)).with(csrf()).param("name","Nuevo Conductor"))
            .andExpect(redirectedUrl("/ui/profile"));
        assertEquals("PENDING",drivers.findByIdentitySubject(newSubject).orElseThrow().getQualificationStatus());
        mvc.perform(get("/ui/profile").with(user(newSubject))).andExpect(status().isOk()).andExpect(content().string(containsString("Nuevo Conductor")));
    }
}
