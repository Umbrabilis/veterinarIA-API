package tech.veterinaria_api.mascotas;

import java.time.LocalDate;
import java.util.UUID;

public record MascotaResponse(UUID id, UUID duenoId, String nombre, String especie,
        String raza, SexoMascota sexo, LocalDate fechaNacimiento) {
    public static MascotaResponse de(Mascota m) {
        return new MascotaResponse(m.getId(), m.getDuenoId(), m.getNombre(), m.getEspecie(),
                m.getRaza(), m.getSexo(), m.getFechaNacimiento());
    }
}
