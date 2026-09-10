package tech.veterinaria_api.mascotas;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.*;

public record MascotaRequest(
    @NotNull UUID duenoId,
    @NotBlank @Size(max = 150) String nombre,
    @NotBlank @Size(max = 80) String especie,
    @Size(max = 100) String raza,
    @NotNull SexoMascota sexo,
    @PastOrPresent LocalDate fechaNacimiento
) {}
