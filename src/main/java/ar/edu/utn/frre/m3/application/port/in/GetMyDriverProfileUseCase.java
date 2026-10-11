package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.domain.Driver;

/**
 * Puerto de entrada (Caso de uso - HU-02): Consultar mi perfil como conductor autenticado.
 * Resuelve la identidad del conductor a partir del contexto seguro de M1 (Keycloak).
 */
public interface GetMyDriverProfileUseCase {

    /**
     * Obtiene el perfil y estado actual del conductor autenticado en la sesión o token actual.
     *
     * @return El perfil del conductor autenticado.
     */
    Driver getMyDriverProfile();
}
