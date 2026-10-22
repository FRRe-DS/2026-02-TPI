package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.adapter.security.RoleNavigation;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds role-aware navigation flags to every Thymeleaf web view. */
@ControllerAdvice
public class UiNavigationModelAdvice {
    private final RoleNavigation navigation;
    private final ObjectProvider<GetMyDriverProfileUseCase> getMyDriverProfile;

    public UiNavigationModelAdvice(ObjectProvider<RoleNavigation> navigation,
                                   ObjectProvider<GetMyDriverProfileUseCase> getMyDriverProfile) {
        this.navigation = navigation.getIfAvailable(RoleNavigation::new);
        this.getMyDriverProfile = getMyDriverProfile;
    }

    @ModelAttribute
    public void addNavigation(Model model, Authentication authentication) {
        boolean hasDriverRole = navigation.hasDriverRole(authentication);
        boolean hasOperatorRole = navigation.hasOperatorRole(authentication);
        model.addAttribute("hasDriverRole", hasDriverRole);
        model.addAttribute("hasOperatorRole", hasOperatorRole);
        model.addAttribute("showCreateProfile", hasDriverRole && !hasDriverProfile());
        model.addAttribute("homePath", navigation.landingPath(authentication));
    }

    private boolean hasDriverProfile() {
        GetMyDriverProfileUseCase useCase = getMyDriverProfile.getIfAvailable();
        if (useCase == null) {
            return false;
        }
        try {
            useCase.getMyDriverProfile();
            return true;
        } catch (DriverNotFoundException ignored) {
            return false;
        }
    }
}
