package tech.veterinaria_api.security;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import tech.veterinaria_api.common.NoAutenticadoException;
import tech.veterinaria_api.common.RolUsuario;

/**
 * Identidad del usuario autenticado, leída de los claims del JWT emitido por auth-service
 * ({@code sub} = email, {@code userId}, {@code rol}).
 */
public record UsuarioActual(UUID id, String email, RolUsuario rol) {

    public static UsuarioActual de(Jwt jwt) {
        if (jwt == null) {
            throw new NoAutenticadoException("Se requiere autenticación");
        }
        try {
            return new UsuarioActual(UUID.fromString(jwt.getClaimAsString("userId")), jwt.getSubject(),
                    RolUsuario.valueOf(jwt.getClaimAsString("rol")));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new NoAutenticadoException("El token no contiene una identidad válida");
        }
    }

    public boolean esPropietario() {
        return rol == RolUsuario.PROPIETARIO;
    }

    public boolean esVeterinario() {
        return rol == RolUsuario.VETERINARIO;
    }

    public boolean esAdministrador() {
        return rol == RolUsuario.ADMINISTRADOR;
    }

    /** Personal de la clínica: administradores y veterinarios. */
    public boolean esPersonalClinico() {
        return esAdministrador() || esVeterinario();
    }
}
