package tech.veterinaria_api.auth.dto;

import java.util.UUID;

import tech.veterinaria_api.usuarios.RolUsuario;
import tech.veterinaria_api.usuarios.Usuario;

public record UsuarioResponse(
        UUID id,
        String nombre,
        String email,
        RolUsuario rol
) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol());
    }
}
