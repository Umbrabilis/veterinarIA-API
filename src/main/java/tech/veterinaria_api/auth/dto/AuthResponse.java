package tech.veterinaria_api.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UsuarioResponse usuario
) {
}
