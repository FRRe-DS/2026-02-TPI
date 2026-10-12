package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.adapter.persistence.entity.*;
import ar.edu.utn.frre.m3.adapter.persistence.repository.*;
import ar.edu.utn.frre.m3.adapter.integration.ExternalModulesClient;
import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CompletedWorkflowsTest {
    @Autowired MockMvc mvc;
    @Autowired SpringDataDriverRepository drivers;
    @Autowired SpringDataVehicleRepository vehicles;
    @Autowired DocumentRepository documents;
    @Autowired AvailabilityDeliveryPort deliveries;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entities;
    @SpyBean ExternalModulesClient external;
    UUID driver,vehicle; String subject,path;
    @BeforeEach void setup() {
        driver=UUID.randomUUID();vehicle=UUID.randomUUID();subject="workflow-"+driver;path="/api/v1/drivers/"+driver;
        Instant now=Instant.now();
        drivers.saveAndFlush(new DriverJpaEntity(driver,subject,"Prueba","PENDING",1L,now,now));
        vehicles.saveAndFlush(new VehicleJpaEntity(vehicle,driver,"AUTO","AUTO","AR","TEST01","Test","Test",2023,"PENDING",1L,now,now));
    }
    void seedDocuments() {
        Instant now=Instant.now();
        var license=Document.createPendingLicense(driver,"TEST-L",now.minusSeconds(60),now.plusSeconds(86400),Set.of(ServiceType.AUTO),null,1,1);
        license.review(ReviewDecision.APPROVED,"Verificado","operator",1);documents.save(license);
        for(var type:List.of(DocumentType.INSURANCE,DocumentType.VEHICLE_REGISTRATION)) {
            var doc=Document.createPendingRevision(driver,vehicle,type,"TEST-D","Test",now.minusSeconds(60),now.plusSeconds(86400),null,1);
            doc.review(ReviewDecision.APPROVED,"Verificado","operator",1);documents.save(doc);
        }
        entities.flush();
    }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder decision(String suffix,String status,long version,String key) {
        return post(path+suffix).with(user("operator").roles("OPERATOR")).header("If-Match","\""+version+"\"")
            .header("Idempotency-Key",key).contentType("application/json").content("{\"status\":\""+status+"\",\"reason\":\"Verificación de prueba\"}");
    }
    @Test void qualificationRequiresDocumentsRoleVersionAndKeepsAnIdempotentHistory() throws Exception {
        String key=UUID.randomUUID().toString();
        mvc.perform(decision("/qualification","APPROVED",1,key)).andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("select count(*) from qualification_decisions where driver_id=?",Integer.class,driver));
        seedDocuments();
        mvc.perform(decision("/qualification","APPROVED",1,key).with(user(subject).roles("DRIVER"))).andExpect(status().isForbidden());
        for(int n=0;n<2;n++) mvc.perform(decision("/qualification","APPROVED",1,key)).andExpect(status().isOk()).andExpect(header().string("ETag","\"2\""));
        mvc.perform(decision("/qualification","SUSPENDED",2,key)).andExpect(status().isConflict());
        mvc.perform(decision("/vehicles/"+vehicle+"/qualification","APPROVED",1,UUID.randomUUID().toString())).andExpect(status().isPreconditionFailed());
        mvc.perform(decision("/vehicles/"+vehicle+"/qualification","APPROVED",2,UUID.randomUUID().toString())).andExpect(status().isOk()).andExpect(header().string("ETag","\"3\""));
        mvc.perform(get(path+"/qualification/history").with(user(subject))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertEquals(2,jdbc.queryForObject("select count(*) from qualification_decisions where driver_id=?",Integer.class,driver));
        assertEquals(3L,drivers.findById(driver).orElseThrow().getVersion());
    }
    @Test void ratingsValidateOwnershipCompletedTripAndExactlyOneRatingPerTrip() throws Exception {
        doReturn(new TripContext("trip-1",driver,"customer-1","COMPLETED")).when(external).completedTrip("trip-1",driver);
        String key=UUID.randomUUID().toString();
        for(int n=0;n<2;n++) mvc.perform(post(path+"/customer-ratings").with(user(subject)).header("Idempotency-Key",key)
            .contentType("application/json").content("{\"tripId\":\"trip-1\",\"score\":5,\"comment\":\"Excelente\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.clientId").value("customer-1"));
        verify(external,times(1)).completedTrip("trip-1",driver);
        mvc.perform(post(path+"/customer-ratings").with(user(subject)).header("Idempotency-Key",UUID.randomUUID().toString())
            .contentType("application/json").content("{\"tripId\":\"trip-1\",\"score\":4}"))
            .andExpect(status().isConflict());
        mvc.perform(get(path+"/customer-ratings").with(user("other").roles("DRIVER"))).andExpect(status().isForbidden());
        doReturn(new TripContext("trip-2",driver,"customer-2","IN_PROGRESS")).when(external).completedTrip("trip-2",driver);
        mvc.perform(post(path+"/customer-ratings").with(user(subject)).header("Idempotency-Key",UUID.randomUUID().toString())
            .contentType("application/json").content("{\"tripId\":\"trip-2\",\"score\":5}"))
            .andExpect(status().isBadRequest());
        assertEquals(1,jdbc.queryForObject("select count(*) from customer_ratings where driver_id=?",Integer.class,driver));
    }
    @Test void newerAvailabilitySurvivesStaleAcknowledgementAndReplayKeepsOriginalVersion() throws Exception {
        seedDocuments();
        mvc.perform(decision("/qualification","APPROVED",1,UUID.randomUUID().toString())).andExpect(status().isOk());
        mvc.perform(decision("/vehicles/"+vehicle+"/qualification","APPROVED",2,UUID.randomUUID().toString())).andExpect(status().isOk());
        String key=UUID.randomUUID().toString(),body="{\"vehicleId\":\""+vehicle+"\",\"connected\":true,\"available\":true}";
        mvc.perform(put(path+"/availability-intent").with(user(subject)).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isAccepted());
        entities.flush();
        var old=deliveries.claim(Instant.now().plusSeconds(1),Instant.now().plusSeconds(60)).orElseThrow();
        mvc.perform(put(path+"/availability-intent").with(user(subject)).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body.replace("true","false")))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.desiredVersion").value(2));
        entities.flush();
        deliveries.finish(old,true,false,Instant.now(),Instant.now());entities.clear();
        mvc.perform(get(path+"/availability").with(user(subject))).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(put(path+"/availability-intent").with(user(subject)).header("Idempotency-Key",key).contentType("application/json").content(body))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.desiredVersion").value(1)).andExpect(jsonPath("$.connected").value(true));
        var latest=deliveries.claim(Instant.now().plusSeconds(1),Instant.now().plusSeconds(60)).orElseThrow();
        deliveries.finish(latest,false,true,Instant.now(),Instant.now());
        deliveries.retry(driver,Instant.now());
        var retried=deliveries.claim(Instant.now().plusSeconds(1),Instant.now().plusSeconds(60)).orElseThrow();
        assertEquals(latest.id(),retried.id());
        deliveries.finish(retried,true,false,Instant.now(),Instant.now());entities.clear();
        mvc.perform(get(path+"/availability").with(user(subject))).andExpect(jsonPath("$.status").value("APPLIED"));
        assertEquals(5L,drivers.findById(driver).orElseThrow().getVersion());
    }
}
