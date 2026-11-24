package tech.veterinaria_api.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.veterinaria_api.common.Patrones;
import tech.veterinaria_api.common.RolUsuario;

/** Un administrador crea la cuenta de otro miembro del personal con una contraseña inicial. */
public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, puntos, apóstrofos y guiones")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(regexp = Patrones.EMAIL, message = "El email no tiene un formato válido")
        @Size(max = 100, message = "El email no puede superar 100 caracteres")
        String email,

        @NotBlank(message = "La contraseña inicial es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = Patrones.PASSWORD, message = "La contraseña debe incluir al menos una letra y un número")
        String password,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {
}
