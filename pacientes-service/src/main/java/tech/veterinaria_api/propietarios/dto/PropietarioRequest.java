package tech.veterinaria_api.propietarios.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un propietario por el personal de la clínica. {@code aceptaTratamientoDatos} registra la
 * autorización del titular exigida por la Ley 1581 de 2012; sin ella no se guarda ningún dato.
 */
public record PropietarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 30, message = "El documento no puede superar 30 caracteres")
        String documento,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^[0-9+()\\s-]{7,30}$", message = "El teléfono no tiene un formato válido")
        String telefono,

        @Email(message = "El email no tiene un formato válido")
        @Size(max = 255, message = "El email no puede superar 255 caracteres")
        String email,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion,

        @AssertTrue(message = "Se requiere la autorización de tratamiento de datos personales (Ley 1581 de 2012)")
        boolean aceptaTratamientoDatos
) {
}
