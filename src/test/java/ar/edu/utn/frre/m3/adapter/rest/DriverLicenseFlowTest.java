package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.domain.Document;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DriverLicenseFlowTest {
    @Autowired MockMvc mvc;
    @Autowired SpringDataDriverRepository drivers;
    @Autowired DocumentRepository documents;

    @Test
    void apiAndWebPersistRenewalHistory() throws Exception {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        drivers.save(new DriverJpaEntity(id, "subject-" + id, "Conductor Prueba",
                "PENDING", 1L, now, now));
        String path = "/api/v1/drivers/" + id + "/documents";
        String payload = """
                {"documentNumber":"LIC-123","issuedAt":"2026-01-01T00:00:00Z",
                 "validUntil":"2027-01-01T00:00:00Z","serviceTypes":["AUTO"]}
                """;
        String key = "test-license-request-0001";

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1\"").header("Idempotency-Key", key).content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
                .andExpect(jsonPath("$.documentNumber").doesNotExist());

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1\"").header("Idempotency-Key", key).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revision").value(1));

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1\"").header("Idempotency-Key", key)
                        .content(payload.replace("LIC-123", "LIC-OTHER")))
                .andExpect(status().isConflict());

        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1\"").header("Idempotency-Key", "new-license-request-0002")
                        .content(payload))
                .andExpect(status().isPreconditionFailed());

        mvc.perform(post("/ui/drivers/" + id + "/documents")
                        .param("documentNumber", "LIC-456")
                        .param("issuedAt", "2027-01-01")
                        .param("validUntil", "2028-01-01")
                        .param("serviceTypes", "MOTO")
                        .param("expectedVersion", "2"))
                .andExpect(status().is3xxRedirection());

        List<Document> history = documents.findLicensesByDriverId(id);
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getRevision());
        assertTrue(history.get(0).isCurrent());
        assertFalse(history.get(1).isCurrent());
        assertEquals("LIC-123", history.get(1).getDocumentNumber());

        mvc.perform(get("/ui/drivers/" + id + "/documents"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Historial")));
    }
}
