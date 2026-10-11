package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.persistence.SpringDataDriverRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DriverIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataDriverRepository driverRepository;

    private final List<UUID> createdDriverIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (UUID id : createdDriverIds) {
            driverRepository.deleteById(id);
        }
        createdDriverIds.clear();
    }

    @Test
    @DisplayName("Flujo HU-01: Creación exitosa retorna 201 Created con id, estado PENDING, version 1 y auditoría")
    void shouldCreateDriverSuccessfully() throws Exception {
        String identitySubject = "auth0|test-" + UUID.randomUUID();
        String name = "Lionel Messi";
        CreateDriverRequest request = new CreateDriverRequest(identitySubject, name);

        String responseBody = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.identitySubject").value(identitySubject))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.qualificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        DriverResponse response = objectMapper.readValue(responseBody, DriverResponse.class);
        createdDriverIds.add(response.id());

        assertTrue(driverRepository.existsByIdentitySubject(identitySubject));
    }

    @Test
    @DisplayName("Flujo HU-01: Rechazo por duplicado retorna 409 Conflict cuando el identitySubject ya existe")
    void shouldRejectDuplicateDriverWith409Conflict() throws Exception {
        String identitySubject = "auth0|duplicate-" + UUID.randomUUID();
        String name = "Angel Di Maria";
        CreateDriverRequest request = new CreateDriverRequest(identitySubject, name);

        // Primer request exitoso
        String firstResponseBody = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        DriverResponse response = objectMapper.readValue(firstResponseBody, DriverResponse.class);
        createdDriverIds.add(response.id());

        // Segundo request con el mismo identitySubject
        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DRIVER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.title").value("Conflicto de identidad de conductor"));
    }

    @Test
    @DisplayName("Flujo HU-01: Validación de campos obligatorios retorna 400 Bad Request cuando faltan datos")
    void shouldRejectInvalidPayloadWith400BadRequest() throws Exception {
        CreateDriverRequest invalidRequest = new CreateDriverRequest("", "");

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.invalidFields").isMap());
    }
}
