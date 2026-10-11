package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.application.exception.DriverAlreadyExistsException;
import ar.edu.utn.frre.m3.application.usecase.CreateDriverProfileUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DriverRestController.class)
class DriverRestControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CreateDriverProfileUseCase createDriverProfileUseCase;

        @Test
        @DisplayName("POST /api/v1/drivers retorna 201 Created con el conductor creado")
        void shouldReturn201CreatedWhenDriverProfileIsCreated() throws Exception {
                CreateDriverRequest request = new CreateDriverRequest("sub-12345", "Juan Perez");
                Driver createdDriver = new Driver("sub-12345", "Juan Perez");

                when(createDriverProfileUseCase.execute(eq("sub-12345"), eq("Juan Perez")))
                                .thenReturn(createdDriver);

                mockMvc.perform(post("/api/v1/drivers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/api/v1/drivers/" + createdDriver.getId()))
                                .andExpect(jsonPath("$.id").value(createdDriver.getId().toString()))
                                .andExpect(jsonPath("$.identitySubject").value("sub-12345"))
                                .andExpect(jsonPath("$.name").value("Juan Perez"))
                                .andExpect(jsonPath("$.qualificationStatus").value("PENDING"))
                                .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("POST /api/v1/drivers retorna 409 Conflict cuando el identity_subject ya existe")
        void shouldReturn409ConflictWhenDriverAlreadyExists() throws Exception {
                CreateDriverRequest request = new CreateDriverRequest("sub-duplicate", "Juan Perez");

                when(createDriverProfileUseCase.execute(eq("sub-duplicate"), eq("Juan Perez")))
                                .thenThrow(new DriverAlreadyExistsException("sub-duplicate"));

                mockMvc.perform(post("/api/v1/drivers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.title").value("Conflicto de identidad de conductor"))
                                .andExpect(jsonPath("$.code").value("DRIVER_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("POST /api/v1/drivers retorna 400 Bad Request cuando faltan campos requeridos")
        void shouldReturn400BadRequestWhenPayloadIsInvalid() throws Exception {
                CreateDriverRequest invalidRequest = new CreateDriverRequest("", "");

                mockMvc.perform(post("/api/v1/drivers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
}
