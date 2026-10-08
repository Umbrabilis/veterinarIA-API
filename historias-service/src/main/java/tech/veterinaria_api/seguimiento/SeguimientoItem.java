package tech.veterinaria_api.seguimiento;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Algo que la clínica debe recordarle al dueño: un control indicado en una consulta o el refuerzo de una vacuna.
 * {@code referenciaId} es la consulta o la vacuna que lo originó.
 */
public record SeguimientoItem(
        TipoSeguimiento tipo,
        LocalDate fecha,
        UUID mascotaId,
        UUID propietarioId,
        UUID referenciaId,
        String descripcion
) {
    public enum TipoSeguimiento {
        CONTROL,
        REFUERZO_VACUNA
    }
}
