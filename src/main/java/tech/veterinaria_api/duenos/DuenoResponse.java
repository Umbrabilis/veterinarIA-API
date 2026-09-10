package tech.veterinaria_api.duenos;

import java.util.UUID;

public record DuenoResponse(UUID id, String nombre, String email, String telefono, String direccion) {
    public static DuenoResponse de(Dueno d) {
        return new DuenoResponse(d.getId(), d.getNombre(), d.getEmail(), d.getTelefono(), d.getDireccion());
    }
}
