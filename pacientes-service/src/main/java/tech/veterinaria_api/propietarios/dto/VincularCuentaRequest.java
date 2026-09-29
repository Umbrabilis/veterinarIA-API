package tech.veterinaria_api.propietarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VincularCuentaRequest(
        @NotBlank(message = "El código de vinculación es obligatorio")
        @Size(max = 20, message = "El código de vinculación no es válido")
        String codigo
) {
}
