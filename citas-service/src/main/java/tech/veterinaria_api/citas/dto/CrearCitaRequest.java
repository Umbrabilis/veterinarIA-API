package tech.veterinaria_api.citas.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearCitaRequest(
        @NotNull(message = "La mascota es obligatoria")
        UUID mascotaId,

        @NotNull(message = "El veterinario es obligatorio")
        UUID veterinarioId,

        @NotNull(message = "La hora de inicio es obligatoria")
        @Future(message = "La cita debe ser en el futuro")
        Instant inicio,

        @NotNull(message = "La hora de fin es obligatoria")
        Instant fin,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
