package tech.veterinaria_api.usuarios.dto;

import java.time.Instant;
import java.util.UUID;

import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.usuarios.Usuario;

/** Vista de una cuenta del personal para el módulo de usuarios (solo administradores). */
public record UsuarioAdminResponse(
        UUID id,
        String nombre,
        String email,
        RolUsuario rol,
        boolean activo,
        Instant createdAt
) {
    public static UsuarioAdminResponse de(Usuario usuario) {
        return new UsuarioAdminResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(),
                usuario.isActivo(), usuario.getCreatedAt());
    }
}
