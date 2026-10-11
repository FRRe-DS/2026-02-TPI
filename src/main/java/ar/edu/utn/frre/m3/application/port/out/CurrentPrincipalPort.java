package ar.edu.utn.frre.m3.application.port.out;

import java.util.Optional;

/**
 * Puerto de salida para consultar la identidad y roles del usuario autenticado en la solicitud actual.
 * Desacopla la lógica de aplicación del framework Spring Security.
 */
public interface CurrentPrincipalPort {

    /**
     * Retorna el sujeto de identidad (sub claim de Keycloak / M1).
     */
    Optional<String> getSubject();

    /**
     * Comprueba si el usuario autenticado posee un rol determinado (e.g. "ROLE_OPERATOR", "ROLE_DRIVER").
     */
    boolean hasRole(String role);

    /**
     * Comprueba si hay un usuario autenticado activo en el contexto.
     */
    boolean isAuthenticated();
}
