package tech.veterinaria_api.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.veterinaria_api.common.Patrones;
import tech.veterinaria_api.common.RolUsuario;

/** Edición por un administrador. El email no se cambia: es el usuario con el que se inicia sesión. */
public record ActualizarUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        @Pattern(regexp = Patrones.NOMBRE_PERSONA, message = "El nombre solo puede contener letras, espacios, puntos, apóstrofos y guiones")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,

        @NotNull(message = "Indica si la cuenta queda activa")
        Boolean activo
) {
}
