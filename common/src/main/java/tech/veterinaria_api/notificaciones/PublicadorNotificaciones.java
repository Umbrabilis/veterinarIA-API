package tech.veterinaria_api.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

/**
 * Publica avisos en Redis para notificaciones-service. Es "dispara y olvida": si Redis no responde, la operación de
 * negocio (agendar una cita, enviar un resumen) ya ocurrió y no debe fallar por un aviso; solo se registra.
 * Existe únicamente en los servicios que incluyen spring-boot-starter-data-redis.
 */
@Component
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
public class PublicadorNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(PublicadorNotificaciones.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public PublicadorNotificaciones(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public void publicar(EventoNotificacion evento) {
        try {
            String mensaje = json.writeValueAsString(evento);
            // El historial se guarda aquí, una sola vez; las réplicas de notificaciones-service solo lo leen.
            redis.opsForList().leftPush(EventoNotificacion.HISTORIAL, mensaje);
            redis.opsForList().trim(EventoNotificacion.HISTORIAL, 0, EventoNotificacion.MAXIMO_HISTORIAL - 1);
            redis.convertAndSend(EventoNotificacion.CANAL, mensaje);
        } catch (RuntimeException e) {
            log.warn("No se pudo publicar la notificación {} ({})", evento.tipo(), e.getClass().getSimpleName());
        }
    }
}
