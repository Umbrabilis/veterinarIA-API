package tech.veterinaria_api.citas.dto;

import java.time.Instant;
import java.util.UUID;

import tech.veterinaria_api.citas.Cita;
import tech.veterinaria_api.citas.EstadoCita;

public record CitaResponse(
        UUID id,
        UUID mascotaId,
        String mascotaNombre,
        UUID propietarioId,
        UUID veterinarioId,
        String veterinarioNombre,
        Instant inicio,
        Instant fin,
        String motivo,
        EstadoCita estado,
        Instant createdAt
) {
    public static CitaResponse de(Cita c) {
        return new CitaResponse(c.getId(), c.getMascotaId(), c.getMascotaNombre(), c.getPropietarioId(),
                c.getVeterinarioId(), c.getVeterinarioNombre(), c.getInicio(), c.getFin(), c.getMotivo(),
                c.getEstado(), c.getCreatedAt());
    }
}
