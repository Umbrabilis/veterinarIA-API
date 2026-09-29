package tech.veterinaria_api.common;

/**
 * La petición es válida en forma pero viola una regla del dominio (p. ej. editar una consulta cerrada).
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
