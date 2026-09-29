package tech.veterinaria_api.vacunas.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tech.veterinaria_api.common.Patrones;

public record VacunaRequest(
        @NotNull(message = "La mascota es obligatoria")
        UUID mascotaId,

        @NotBlank(message = "El nombre de la vacuna es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        @Pattern(regexp = Patrones.NOMBRE_COSA, message = "El nombre de la vacuna contiene caracteres no permitidos")
        String nombre,

        @Size(max = 60, message = "El lote no puede superar 60 caracteres")
        @Pattern(regexp = Patrones.LOTE, message = "El lote solo puede contener letras, números y guiones")
        String lote,

        @NotNull(message = "La fecha de aplicación es obligatoria")
        @PastOrPresent(message = "La fecha de aplicación no puede estar en el futuro")
        LocalDate fechaAplicacion,

        LocalDate proximaDosis,

        @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
        String notas
) {
}
