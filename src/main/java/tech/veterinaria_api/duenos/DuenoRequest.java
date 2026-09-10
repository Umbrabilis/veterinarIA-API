package tech.veterinaria_api.duenos;

import jakarta.validation.constraints.*;

public record DuenoRequest(
    @NotBlank @Size(max = 150) String nombre,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 30) String telefono,
    @Size(max = 255) String direccion
) {}
