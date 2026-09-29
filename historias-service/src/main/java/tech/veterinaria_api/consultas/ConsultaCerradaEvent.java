package tech.veterinaria_api.consultas;

import java.util.UUID;

/** Se publica cuando una consulta pasa a CERRADA; se procesa después del commit. */
public record ConsultaCerradaEvent(UUID consultaId) {
}
