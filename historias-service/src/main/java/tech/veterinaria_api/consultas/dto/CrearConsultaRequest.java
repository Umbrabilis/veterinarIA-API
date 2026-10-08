package tech.veterinaria_api.consultas.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CrearConsultaRequest(
        @NotNull(message = "La mascota es obligatoria")
        UUID mascotaId,

        UUID citaId,

        @NotNull(message = "Los datos clínicos son obligatorios")
        @Valid
        DatosClinicosRequest datos
) {
}
