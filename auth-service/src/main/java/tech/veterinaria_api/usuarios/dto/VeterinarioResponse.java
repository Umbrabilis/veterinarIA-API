package tech.veterinaria_api.usuarios.dto;

import java.util.UUID;

import tech.veterinaria_api.usuarios.Usuario;

/** Datos de un veterinario para agendar citas. Sin email: no hace falta para elegirlo. */
public record VeterinarioResponse(UUID id, String nombre) {

    public static VeterinarioResponse de(Usuario usuario) {
        return new VeterinarioResponse(usuario.getId(), usuario.getNombre());
    }
}
