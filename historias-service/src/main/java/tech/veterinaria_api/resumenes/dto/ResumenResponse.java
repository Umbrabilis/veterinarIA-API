package tech.veterinaria_api.resumenes.dto;

import java.time.Instant;
import java.util.UUID;

import tech.veterinaria_api.resumenes.ContenidoResumen;
import tech.veterinaria_api.resumenes.EstadoResumen;
import tech.veterinaria_api.resumenes.ResumenConsulta;

/**
 * Para el personal de la clínica incluye lo que generó el modelo y la trazabilidad. Para el dueño
 * ({@link #paraPropietario}) solo el contenido final aprobado.
 */
public record ResumenResponse(
        UUID id,
        UUID consultaId,
        EstadoResumen estado,
        ContenidoResumen generado,
        ContenidoResumen contenidoFinal,
        String modelo,
        String versionPrompt,
        UUID aprobadoPor,
        Instant aprobadoEn,
        Instant enviadoEn,
        Instant createdAt
) {
    public static ResumenResponse de(ResumenConsulta r) {
        return new ResumenResponse(r.getId(), r.getConsultaId(), r.getEstado(), r.generado(), r.contenidoFinal(),
                r.getModelo(), r.getVersionPrompt(), r.getAprobadoPor(), r.getAprobadoEn(), r.getEnviadoEn(),
                r.getCreatedAt());
    }

    public static ResumenResponse paraPropietario(ResumenConsulta r) {
        return new ResumenResponse(r.getId(), r.getConsultaId(), r.getEstado(), null, r.contenidoFinal(), null, null,
                null, r.getAprobadoEn(), r.getEnviadoEn(), r.getCreatedAt());
    }
}
