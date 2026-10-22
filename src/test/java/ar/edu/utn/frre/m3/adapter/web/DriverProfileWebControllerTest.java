package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.adapter.security.RoleNavigation;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import ar.edu.utn.frre.m3.application.port.in.UpdateMyPersonalInfoUseCase;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.PersonalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = DriverProfileWebController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import({RoleNavigation.class, UiNavigationModelAdvice.class})
@ActiveProfiles("test")
@DisplayName("Pruebas MockMvc de Adaptador Web: DriverProfileWebController (HU-02)")
class DriverProfileWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMyDriverProfileUseCase getMyDriverProfileUseCase;

    @MockBean
    private GetDriverProfileUseCase getDriverProfileUseCase;

    @MockBean
    private UpdateMyPersonalInfoUseCase updateMyPersonalInfo;

    private Driver sampleDriver;
    private final UUID driverId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        PersonalInfo info = new PersonalInfo(
                "Lautaro",
                "González",
                "42158963",
                LocalDate.of(1999, 8, 20),
                "Av. Castelli 950",
                "+54 362 4558899",
                "lautaro.gonzalez@example.com"
        );
        sampleDriver = new Driver(driverId, "user-sub-123", info, Instant.now());
    }

    @Test
    @DisplayName("GET /ui debe redirigir a /ui/profile")
    void shouldRedirectFromUiToProfile() throws Exception {
        mockMvc.perform(get("/ui").principal(authentication("driver", "ROLE_DRIVER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/profile"));
    }

    @Test
    @DisplayName("GET /ui redirige al listado para un operador sin rol DRIVER")
    void shouldRedirectOperatorWithoutDriverRoleToDriverManagement() throws Exception {
        mockMvc.perform(get("/ui").principal(authentication("operator", "ROLE_OPERATOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/operator/drivers"));
    }

    @Test
    @DisplayName("GET /ui/profile no busca Driver para un operador sin rol DRIVER")
    void shouldNotLookUpDriverForOperatorOnly() throws Exception {
        mockMvc.perform(get("/ui/profile").principal(authentication("operator", "ROLE_OPERATOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/operator/drivers"));
        org.mockito.Mockito.verifyNoInteractions(getMyDriverProfileUseCase);
    }

    private Authentication authentication(String name, String... authorities) {
        return new UsernamePasswordAuthenticationToken(name, "n/a", AuthorityUtils.createAuthorityList(authorities));
    }

    @Test
    @DisplayName("GET /ui/profile debe retornar vista profile con atributos de modelo 200 OK")
    void shouldRenderProfileViewSuccessfully() throws Exception {
        when(getMyDriverProfileUseCase.getMyDriverProfile()).thenReturn(sampleDriver);

        mockMvc.perform(get("/ui/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("driver"))
                .andExpect(model().attribute("driver", org.hamcrest.Matchers.hasProperty("fullName", org.hamcrest.Matchers.is("Lautaro González"))))
                .andExpect(model().attribute("driver", org.hamcrest.Matchers.hasProperty("dni", org.hamcrest.Matchers.is("42158963"))));
    }

    @Test
    @DisplayName("GET /ui/profile con conductor no encontrado debe retornar vista de error amigable")
    void shouldRenderErrorViewWhenDriverNotFound() throws Exception {
        when(getMyDriverProfileUseCase.getMyDriverProfile())
                .thenThrow(new DriverNotFoundException("No se encontró ningún perfil de conductor registrado."));

        mockMvc.perform(get("/ui/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /ui/profile/edit muestra el formulario con los datos actuales")
    void shouldRenderPersonalInfoEditForm() throws Exception {
        when(getMyDriverProfileUseCase.getMyDriverProfile()).thenReturn(sampleDriver);

        mockMvc.perform(get("/ui/profile/edit").principal(authentication("driver", "ROLE_DRIVER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attribute("form", org.hamcrest.Matchers.hasProperty("dni", org.hamcrest.Matchers.is("42158963"))));
    }

    @Test
    @DisplayName("POST /ui/profile/edit guarda los datos personales y vuelve al perfil")
    void shouldUpdatePersonalInfo() throws Exception {
        mockMvc.perform(post("/ui/profile/edit").principal(authentication("driver", "ROLE_DRIVER"))
                        .param("firstName", "Lautaro")
                        .param("lastName", "González")
                        .param("dni", "42158963")
                        .param("birthDate", "1999-08-20")
                        .param("address", "Av. Castelli 950")
                        .param("phone", "+54 362 4558899")
                        .param("email", "lautaro.gonzalez@example.com")
                        .param("version", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/profile"))
                .andExpect(flash().attribute("success", "Datos personales actualizados."));

        org.mockito.Mockito.verify(updateMyPersonalInfo).updatePersonalInfo(
                org.mockito.ArgumentMatchers.any(PersonalInfo.class), org.mockito.ArgumentMatchers.eq(1L));
    }
}
