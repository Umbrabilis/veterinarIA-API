package tech.veterinaria_api.mascotas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import tech.veterinaria_api.mascotas.Especie;
import tech.veterinaria_api.mascotas.Mascota;
import tech.veterinaria_api.mascotas.Sexo;

public record MascotaResponse(
        UUID id,
        UUID propietarioId,
        String nombre,
        Especie especie,
        String raza,
        Sexo sexo,
        LocalDate fechaNacimiento,
        BigDecimal pesoKg,
        String color,
        boolean activo,
        Instant createdAt
) {
    public static MascotaResponse de(Mascota m) {
        return new MascotaResponse(m.getId(), m.getPropietarioId(), m.getNombre(), m.getEspecie(), m.getRaza(),
                m.getSexo(), m.getFechaNacimiento(), m.getPesoKg(), m.getColor(), m.isActivo(), m.getCreatedAt());
    }
}
