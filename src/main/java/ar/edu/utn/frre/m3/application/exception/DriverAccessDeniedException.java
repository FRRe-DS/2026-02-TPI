package ar.edu.utn.frre.m3.application.exception;

/**
 * Excepción lanzada cuando un usuario intenta acceder o consultar los datos de un conductor
 * sin poseer los permisos de propietario ni el rol administrativo de Operador.
 */
public class DriverAccessDeniedException extends RuntimeException {

    public DriverAccessDeniedException(String message) {
        super(message);
    }
}
