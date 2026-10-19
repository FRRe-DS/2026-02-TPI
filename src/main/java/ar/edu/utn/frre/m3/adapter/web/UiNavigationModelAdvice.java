package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.adapter.security.RoleNavigation;
import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.in.GetMyDriverProfileUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds role-aware navigation flags to every Thymeleaf web view. */
@ControllerAdvice
public class UiNavigationModelAdvice {
    private final RoleNavigation navigation;
    private final GetMyDriverProfileUseCase getMyDriverProfile;

    public UiNavigationModelAdvice(RoleNavigation navigation, GetMyDriverProfileUseCase getMyDriverProfile) {
        this.navigation = navigation;
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
        try {
            getMyDriverProfile.getMyDriverProfile();
            return true;
        } catch (DriverNotFoundException ignored) {
            return false;
        }
    }
}
