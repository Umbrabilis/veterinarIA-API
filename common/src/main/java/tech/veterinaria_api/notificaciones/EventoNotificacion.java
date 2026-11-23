package tech.veterinaria_api.notificaciones;

import java.time.Instant;
import java.util.UUID;

/**
 * Aviso para el personal de la clínica que viaja por Redis (canal {@link #CANAL}) hasta notificaciones-service,
 * que lo empuja por WebSocket. {@code destinatarioId} nulo significa "para todo el personal". Nunca lleva datos
 * personales del propietario (Ley 1581): solo nombres de mascota, veterinario y fechas.
 */
public record EventoNotificacion(
        UUID id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        UUID destinatarioId,
        UUID referenciaId,
        Instant fecha
) {
    public static final String CANAL = "veterinaria.notificaciones";
    /** Lista de Redis con los últimos avisos (para quien se conecta después de que ocurrieron). */
    public static final String HISTORIAL = "veterinaria.notificaciones.recientes";
    public static final int MAXIMO_HISTORIAL = 200;

    public static EventoNotificacion para(UUID destinatarioId, TipoNotificacion tipo, String titulo, String mensaje,
            UUID referenciaId) {
        return new EventoNotificacion(UUID.randomUUID(), tipo, titulo, mensaje, destinatarioId, referenciaId,
                Instant.now());
    }

    public static EventoNotificacion paraTodos(TipoNotificacion tipo, String titulo, String mensaje, UUID referenciaId) {
        return para(null, tipo, titulo, mensaje, referenciaId);
    }
}
