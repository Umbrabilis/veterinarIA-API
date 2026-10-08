package tech.veterinaria_api.vacunas.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import tech.veterinaria_api.vacunas.Vacuna;

public record VacunaResponse(
        UUID id,
        UUID mascotaId,
        UUID veterinarioId,
        String nombre,
        String lote,
        LocalDate fechaAplicacion,
        LocalDate proximaDosis,
        String notas,
        Instant createdAt
) {
    public static VacunaResponse de(Vacuna v) {
        return new VacunaResponse(v.getId(), v.getMascotaId(), v.getVeterinarioId(), v.getNombre(), v.getLote(),
                v.getFechaAplicacion(), v.getProximaDosis(), v.getNotas(), v.getCreatedAt());
    }
}
