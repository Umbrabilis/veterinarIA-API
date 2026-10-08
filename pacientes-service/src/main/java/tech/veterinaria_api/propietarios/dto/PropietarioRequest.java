package tech.veterinaria_api.propietarios.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tech.veterinaria_api.common.Patrones;

/**
 * Alta o edición de un propietario por el personal de la clínica. {@code aceptaTratamientoDatos} registra la
 * autorización del titular exigida por la Ley 1581 de 2012; sin ella no se guarda ningún dato.
 */
public record PropietarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, puntos, apóstrofos y guiones")
        String nombre,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 30, message = "El documento no puede superar 30 caracteres")
        @Pattern(regexp = Patrones.DOCUMENTO, message = "El documento solo puede contener letras, números, puntos y guiones (mínimo 3)")
        String documento,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono no tiene un formato válido")
        String telefono,

        @NotBlank(message = "El email es obligatorio: es el canal por el que el propietario recibe las novedades de su mascota")
        @Email(regexp = Patrones.EMAIL, message = "El email no tiene un formato válido")
        @Size(max = 100, message = "El email no puede superar 100 caracteres")
        String email,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        @Pattern(regexp = Patrones.DIRECCION, message = "La dirección contiene caracteres no permitidos")
        String direccion,

        @AssertTrue(message = "Se requiere la autorización de tratamiento de datos personales (Ley 1581 de 2012)")
        boolean aceptaTratamientoDatos
) {
}
