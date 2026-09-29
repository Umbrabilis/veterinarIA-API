package tech.veterinaria_api.citas.dto;

import jakarta.validation.constraints.NotNull;
import tech.veterinaria_api.citas.EstadoCita;

public record CambiarEstadoCitaRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoCita estado
) {
}
