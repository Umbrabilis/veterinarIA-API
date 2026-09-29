package tech.veterinaria_api.remoto;

import java.util.UUID;

public interface UsuariosClient {

    /** Veterinario activo; lanza RecursoNoEncontradoException si el id no corresponde a uno. */
    VeterinarioRemoto obtenerVeterinario(UUID veterinarioId);

    record VeterinarioRemoto(UUID id, String nombre) {
    }
}
