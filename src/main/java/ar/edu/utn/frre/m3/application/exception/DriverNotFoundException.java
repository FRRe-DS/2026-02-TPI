package ar.edu.utn.frre.m3.application.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra un perfil de conductor en el sistema.
 */
public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(String message) {
        super(message);
    }

    public static DriverNotFoundException forId(UUID driverId) {
        return new DriverNotFoundException("No se encontró ningún conductor registrado con el ID: " + driverId);
    }

    public static DriverNotFoundException forSubject(String identitySubject) {
        return new DriverNotFoundException("No se encontró ningún perfil de conductor vinculado a la identidad: " + identitySubject);
    }
}
