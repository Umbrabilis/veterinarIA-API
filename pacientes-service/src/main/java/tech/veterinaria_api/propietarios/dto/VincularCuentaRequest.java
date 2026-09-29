package tech.veterinaria_api.propietarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tech.veterinaria_api.common.Patrones;

public record VincularCuentaRequest(
        @NotBlank(message = "El código de vinculación es obligatorio")
        @Size(max = 20, message = "El código de vinculación no es válido")
        @Pattern(regexp = Patrones.CODIGO_VINCULACION, message = "El código de vinculación no es válido")
        String codigo
) {
}
