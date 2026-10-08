package tech.veterinaria_api.consultas.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import tech.veterinaria_api.consultas.Consulta;
import tech.veterinaria_api.consultas.Enmienda;
import tech.veterinaria_api.consultas.EstadoConsulta;

public record ConsultaResponse(
        UUID id,
        UUID mascotaId,
        UUID propietarioId,
        UUID veterinarioId,
        UUID citaId,
        String motivo,
        String sintomas,
        String examenFisico,
        BigDecimal pesoKg,
        BigDecimal temperaturaC,
        String diagnostico,
        String tratamiento,
        String indicaciones,
        LocalDate proximoControl,
        EstadoConsulta estado,
        Instant cerradaEn,
        Instant createdAt,
        List<EnmiendaResponse> enmiendas
) {
    public static ConsultaResponse de(Consulta c) {
        return de(c, List.of());
    }

    public static ConsultaResponse de(Consulta c, List<Enmienda> enmiendas) {
        return new ConsultaResponse(c.getId(), c.getMascotaId(), c.getPropietarioId(), c.getVeterinarioId(),
                c.getCitaId(), c.getMotivo(), c.getSintomas(), c.getExamenFisico(), c.getPesoKg(), c.getTemperaturaC(),
                c.getDiagnostico(), c.getTratamiento(), c.getIndicaciones(), c.getProximoControl(), c.getEstado(),
                c.getCerradaEn(), c.getCreatedAt(), enmiendas.stream().map(EnmiendaResponse::de).toList());
    }
}
