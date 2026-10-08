package tech.veterinaria_api.auth.dto;

/**
 * Respuesta del login. No incluye el JWT: viaja en una cookie HttpOnly que el JavaScript del frontend no puede leer.
 * {@code expiresInSeconds} indica cuánto dura la sesión.
 */
public record AuthResponse(
        long expiresInSeconds,
        UsuarioResponse usuario
) {
}
