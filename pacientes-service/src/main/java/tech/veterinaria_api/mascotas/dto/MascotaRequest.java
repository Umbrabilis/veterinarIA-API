package tech.veterinaria_api.mascotas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import tech.veterinaria_api.mascotas.Especie;
import tech.veterinaria_api.mascotas.Sexo;

public record MascotaRequest(
        @NotNull(message = "El propietario es obligatorio")
        UUID propietarioId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotNull(message = "La especie es obligatoria")
        Especie especie,

        @Size(max = 100, message = "La raza no puede superar 100 caracteres")
        String raza,

        @NotNull(message = "El sexo es obligatorio")
        Sexo sexo,

        @PastOrPresent(message = "La fecha de nacimiento no puede estar en el futuro")
        LocalDate fechaNacimiento,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
        @DecimalMax(value = "9999.99", message = "El peso no es válido")
        @Digits(integer = 4, fraction = 2, message = "El peso admite máximo dos decimales")
        BigDecimal pesoKg,

        @Size(max = 60, message = "El color no puede superar 60 caracteres")
        String color,

        Boolean activo
) {
}
