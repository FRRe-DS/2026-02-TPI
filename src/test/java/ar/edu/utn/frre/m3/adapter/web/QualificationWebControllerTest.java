package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.CheckDriverAptitudeUseCase;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityReason;
import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = QualificationWebController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Pruebas MockMvc de Adaptador Web: QualificationWebController (HU-09)")
class QualificationWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckDriverAptitudeUseCase checkDriverAptitudeUseCase;

    @Test
    void aptitude_whenEligible_rendersAptitudeViewWithEligibleTrue() throws Exception {
        EligibilityResult result = new EligibilityResult(
                true,
                List.of(),
                Instant.now(),
                1,
                Instant.now().plusSeconds(86400)
        );

        when(checkDriverAptitudeUseCase.executeForSelf()).thenReturn(result);

        mockMvc.perform(get("/ui/qualification"))
                .andExpect(status().isOk())
                .andExpect(view().name("qualification/aptitude"))
                .andExpect(model().attribute("eligible", true))
                .andExpect(model().attributeExists("reasonDescriptions"));
    }

    @Test
    void aptitude_whenNotEligible_rendersAptitudeViewWithReasons() throws Exception {
        EligibilityResult result = new EligibilityResult(
                false,
                List.of(EligibilityReason.LICENSE_EXPIRED),
                Instant.now(),
                1,
                Instant.now()
        );

        when(checkDriverAptitudeUseCase.executeForSelf()).thenReturn(result);

        mockMvc.perform(get("/ui/qualification"))
                .andExpect(status().isOk())
                .andExpect(view().name("qualification/aptitude"))
                .andExpect(model().attribute("eligible", false))
                .andExpect(model().attribute("reasons", List.of(EligibilityReason.LICENSE_EXPIRED)));
    }

    @Test
    void aptitude_whenNoProfile_rendersAptitudeViewWithNoProfile() throws Exception {
        when(checkDriverAptitudeUseCase.executeForSelf())
                .thenThrow(DriverNotFoundException.forSubject("nuevo_conductor"));

        mockMvc.perform(get("/ui/qualification"))
                .andExpect(status().isOk())
                .andExpect(view().name("qualification/aptitude"))
                .andExpect(model().attribute("noProfile", true));
    }

    @Test
    void aptitude_withDriverIdParam_callsExecuteWithDriverId() throws Exception {
        UUID driverId = UUID.randomUUID();
        EligibilityResult result = new EligibilityResult(
                true,
                List.of(),
                Instant.now(),
                1,
                Instant.now().plusSeconds(86400)
        );

        when(checkDriverAptitudeUseCase.execute(driverId)).thenReturn(result);

        mockMvc.perform(get("/ui/qualification").param("driverId", driverId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("qualification/aptitude"))
                .andExpect(model().attribute("eligible", true));
    }
}
