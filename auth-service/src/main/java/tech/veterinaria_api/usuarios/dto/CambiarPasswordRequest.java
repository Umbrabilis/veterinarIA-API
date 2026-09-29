package tech.veterinaria_api.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, message = "La contraseña nueva debe tener al menos 8 caracteres")
        String passwordNueva
) {
}
