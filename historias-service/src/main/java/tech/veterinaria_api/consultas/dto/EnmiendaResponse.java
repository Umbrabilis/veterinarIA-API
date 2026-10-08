package tech.veterinaria_api.consultas.dto;

import java.time.Instant;
import java.util.UUID;

import tech.veterinaria_api.consultas.Enmienda;

public record EnmiendaResponse(UUID id, UUID consultaId, UUID autorId, String motivo, String contenido,
        Instant createdAt) {

    public static EnmiendaResponse de(Enmienda e) {
        return new EnmiendaResponse(e.getId(), e.getConsultaId(), e.getAutorId(), e.getMotivo(), e.getContenido(),
                e.getCreatedAt());
    }
}
