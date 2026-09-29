package tech.veterinaria_api.common;

/**
 * Otro microservicio no respondió o respondió con un error inesperado.
 */
public class ServicioRemotoException extends RuntimeException {

    public ServicioRemotoException(String mensaje) {
        super(mensaje);
    }

    public ServicioRemotoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
