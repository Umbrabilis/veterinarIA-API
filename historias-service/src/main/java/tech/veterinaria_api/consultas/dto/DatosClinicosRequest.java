package tech.veterinaria_api.consultas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contenido clínico editable mientras la consulta está abierta. */
public record DatosClinicosRequest(
        @NotBlank(message = "El motivo de consulta es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo,

        @Size(max = 10000, message = "Los síntomas no pueden superar 10000 caracteres")
        String sintomas,

        @Size(max = 10000, message = "El examen físico no puede superar 10000 caracteres")
        String examenFisico,

        @DecimalMin(value = "0.01", message = "El peso debe ser mayor que cero")
        @Digits(integer = 4, fraction = 2, message = "El peso admite máximo dos decimales")
        BigDecimal pesoKg,

        @DecimalMin(value = "25.0", message = "La temperatura no es válida")
        @DecimalMax(value = "45.0", message = "La temperatura no es válida")
        @Digits(integer = 2, fraction = 1, message = "La temperatura admite un decimal")
        BigDecimal temperaturaC,

        @Size(max = 10000, message = "El diagnóstico no puede superar 10000 caracteres")
        String diagnostico,

        @Size(max = 10000, message = "El tratamiento no puede superar 10000 caracteres")
        String tratamiento,

        @Size(max = 10000, message = "Las indicaciones no pueden superar 10000 caracteres")
        String indicaciones,

        @FutureOrPresent(message = "El próximo control no puede estar en el pasado")
        LocalDate proximoControl
) {
}
