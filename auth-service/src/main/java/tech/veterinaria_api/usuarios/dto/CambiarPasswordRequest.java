package tech.veterinaria_api.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tech.veterinaria_api.common.Patrones;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        @Size(max = 72, message = "La contraseña actual no puede superar 72 caracteres")
        String passwordActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña nueva debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = Patrones.PASSWORD, message = "La contraseña nueva debe incluir al menos una letra y un número")
        String passwordNueva
) {
}
