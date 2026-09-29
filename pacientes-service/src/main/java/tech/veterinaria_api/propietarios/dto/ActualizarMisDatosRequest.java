package tech.veterinaria_api.propietarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import tech.veterinaria_api.common.Patrones;

/**
 * Datos de contacto que el propio propietario puede corregir desde su perfil. Nombre y documento solo los
 * cambia la clínica; el email se gestiona desde la cuenta de acceso.
 */
public record ActualizarMisDatosRequest(
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono no tiene un formato válido")
        String telefono,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        @Pattern(regexp = Patrones.DIRECCION, message = "La dirección contiene caracteres no permitidos")
        String direccion
) {
}
