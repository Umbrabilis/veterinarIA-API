package tech.veterinaria_api.remoto;

import java.util.Optional;
import java.util.UUID;

/**
 * Consultas a pacientes-service con el JWT del usuario: pacientes-service decide si el usuario puede ver la
 * mascota (403 si no es suya), así que una respuesta correcta implica pertenencia.
 */
public interface PacientesClient {

    MascotaRemota obtenerMascota(UUID mascotaId);

    /** Id del propietario de la cuenta autenticada, o vacío si aún no tiene registro en la clínica. */
    Optional<UUID> miPropietarioId();

    record MascotaRemota(UUID id, UUID propietarioId, String nombre, boolean activo) {
    }
}
