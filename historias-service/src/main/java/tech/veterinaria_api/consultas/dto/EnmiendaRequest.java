package tech.veterinaria_api.consultas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnmiendaRequest(
        @NotBlank(message = "El motivo de la enmienda es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo,

        @NotBlank(message = "El contenido de la enmienda es obligatorio")
        @Size(max = 10000, message = "El contenido no puede superar 10000 caracteres")
        String contenido
) {
}
