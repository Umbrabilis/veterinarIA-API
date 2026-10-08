package tech.veterinaria_api.citas.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record ReprogramarCitaRequest(
        @NotNull(message = "La hora de inicio es obligatoria")
        @Future(message = "La cita debe ser en el futuro")
        Instant inicio,

        @NotNull(message = "La hora de fin es obligatoria")
        Instant fin
) {
}
