package ar.edu.utn.frre.m3.adapter.security;

import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa el puerto CurrentPrincipalPort abstrayendo la fuente de autenticación de Spring Security.
 */
@Component
public class SecurityCurrentPrincipalAdapter implements CurrentPrincipalPort {

    @Value("${app.security.mock-subject:}")
    private String mockSubject;

    @Value("${app.security.mock-roles:ROLE_DRIVER,ROLE_OPERATOR}")
    private String mockRoles;

    @Override
    public Optional<String> getSubject() {
        // 1. Soporte para desarrollo / testing si se envía header explícito o atributo de sesión
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String devSubject = request.getHeader("X-Dev-Subject");
            if (devSubject != null && !devSubject.isBlank()) {
                return Optional.of(devSubject.trim());
            }

            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                Object simSubject = session.getAttribute("SIMULATED_SUBJECT");
                if (simSubject != null && !simSubject.toString().isBlank()) {
                    return Optional.of(simSubject.toString().trim());
                }
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            if (mockSubject != null && !mockSubject.isBlank()) {
                return Optional.of(mockSubject);
            }
            return Optional.empty();
        }

        // 2. Token JWT (API REST - Resource Server)
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return Optional.ofNullable(jwt.getSubject());
        }

        // 3. Sesión Web OIDC / OAuth2
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return Optional.ofNullable(oidcUser.getSubject());
        }

        // 4. Autenticación genérica de Spring Security (útil en pruebas @WithMockUser)
        return Optional.ofNullable(authentication.getName());
    }

    @Override
    public boolean hasRole(String role) {
        if (role == null) return false;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String devRoles = request.getHeader("X-Dev-Roles");
            if (devRoles != null && devRoles.contains(role)) {
                return true;
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            if (mockRoles != null && !mockRoles.isBlank()) {
                String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                String simpleRole = role.startsWith("ROLE_") ? role.substring(5) : role;
                for (String r : mockRoles.split(",")) {
                    String trimmed = r.trim();
                    if (trimmed.equalsIgnoreCase(targetRole) || trimmed.equalsIgnoreCase(simpleRole) || trimmed.equalsIgnoreCase(role)) {
                        return true;
                    }
                }
            }
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String targetRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        String simpleRole = role.startsWith("ROLE_") ? role.substring(5) : role;

        return authorities.contains(targetRole) || authorities.contains(simpleRole) || authorities.contains(role);
    }

    @Override
    public boolean isAuthenticated() {
        return getSubject().isPresent();
    }
}
