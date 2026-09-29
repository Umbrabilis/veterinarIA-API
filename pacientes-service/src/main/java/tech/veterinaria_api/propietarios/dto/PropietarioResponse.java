package tech.veterinaria_api.propietarios.dto;

import java.time.Instant;
import java.util.UUID;

import tech.veterinaria_api.propietarios.Propietario;

public record PropietarioResponse(
        UUID id,
        UUID usuarioId,
        String nombre,
        String documento,
        String telefono,
        String email,
        String direccion,
        Instant consentimientoDatosEn
) {
    public static PropietarioResponse de(Propietario p) {
        return new PropietarioResponse(p.getId(), p.getUsuarioId(), p.getNombre(), p.getDocumento(), p.getTelefono(),
                p.getEmail(), p.getDireccion(), p.getConsentimientoDatosEn());
    }
}
