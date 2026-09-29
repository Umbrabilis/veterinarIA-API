package tech.veterinaria_api.vacunas.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record VacunaRequest(
        @NotNull(message = "La mascota es obligatoria")
        UUID mascotaId,

        @NotBlank(message = "El nombre de la vacuna es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 60, message = "El lote no puede superar 60 caracteres")
        String lote,

        @NotNull(message = "La fecha de aplicación es obligatoria")
        @PastOrPresent(message = "La fecha de aplicación no puede estar en el futuro")
        LocalDate fechaAplicacion,

        LocalDate proximaDosis,

        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
        String notas
) {
}
