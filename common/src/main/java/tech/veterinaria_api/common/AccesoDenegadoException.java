package tech.veterinaria_api.common;

/**
 * Autorización por pertenencia: el usuario está autenticado y tiene el rol, pero el recurso no es suyo.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException() {
        super("No tienes permiso para acceder a este recurso");
    }

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
