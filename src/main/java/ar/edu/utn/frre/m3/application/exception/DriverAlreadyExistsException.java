package ar.edu.utn.frre.m3.application.exception;

/**
 * Excepción lanzada cuando ya existe un conductor con el identity_subject indicado.
 */
public class DriverAlreadyExistsException extends RuntimeException {

    private final String identitySubject;

    public DriverAlreadyExistsException(String identitySubject) {
        super("Ya existe un perfil de conductor registrado para la identidad: " + identitySubject);
        this.identitySubject = identitySubject;
    }

    public String getIdentitySubject() {
        return identitySubject;
    }
}
