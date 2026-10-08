package tech.veterinaria_api.remoto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Consultas a pacientes-service con el JWT del usuario: pacientes-service decide si el usuario puede ver la
 * mascota (403 si no es suya), así que una respuesta correcta implica pertenencia.
 */
public interface PacientesClient {

    MascotaRemota obtenerMascota(UUID mascotaId);

    /** Datos de contacto del propietario. Solo para notificarle; nunca se envían al modelo de IA. */
    PropietarioRemoto obtenerPropietario(UUID propietarioId);

    record MascotaRemota(UUID id, UUID propietarioId, String nombre, String especie, String raza, String sexo,
            LocalDate fechaNacimiento, BigDecimal pesoKg, boolean activo) {
    }

    record PropietarioRemoto(UUID id, String nombre, String email) {
    }
}
