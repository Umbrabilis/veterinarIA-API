package tech.veterinaria_api.resumenes;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Las cuatro partes del resumen para el dueño. También es el esquema de salida estructurada que se le pide al
 * modelo (las descripciones viajan en el JSON Schema).
 */
public record ContenidoResumen(
        @JsonPropertyDescription("Qué se encontró en la consulta, en lenguaje sencillo y sin tecnicismos")
        @NotBlank(message = "La sección 'qué se encontró' es obligatoria")
        @Size(max = 4000, message = "Cada sección admite máximo 4000 caracteres")
        String hallazgos,

        @JsonPropertyDescription("Qué tratamiento recibió o debe recibir la mascota, con dosis y horarios si se indicaron")
        @NotBlank(message = "La sección 'tratamiento' es obligatoria")
        @Size(max = 4000, message = "Cada sección admite máximo 4000 caracteres")
        String tratamiento,

        @JsonPropertyDescription("Cuidados que el dueño debe tener en casa y señales de alarma para volver antes")
        @NotBlank(message = "La sección 'cuidados en casa' es obligatoria")
        @Size(max = 4000, message = "Cada sección admite máximo 4000 caracteres")
        String cuidadosEnCasa,

        @JsonPropertyDescription("Cuándo debe volver a control; si no hay fecha indicada, decir que el veterinario lo indicará")
        @NotBlank(message = "La sección 'cuándo volver' es obligatoria")
        @Size(max = 4000, message = "Cada sección admite máximo 4000 caracteres")
        String proximaVisita
) {
}
