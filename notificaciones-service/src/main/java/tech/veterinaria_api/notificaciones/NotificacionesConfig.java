package tech.veterinaria_api.notificaciones;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import tools.jackson.databind.json.JsonMapper;

/**
 * WebSocket en {@value #RUTA_WS} (bajo /api para que el navegador envíe la cookie de sesión, que tiene Path=/api) y
 * suscripción al canal de Redis donde los demás servicios publican los eventos.
 */
@Configuration
@EnableWebSocket
public class NotificacionesConfig implements WebSocketConfigurer {

    public static final String RUTA_WS = "/api/v1/notificaciones/ws";

    private static final Logger log = LoggerFactory.getLogger(NotificacionesConfig.class);

    private final NotificacionesWebSocketHandler handler;
    private final String[] origenesPermitidos;

    public NotificacionesConfig(NotificacionesWebSocketHandler handler,
            @Value("${app.cors.allowed-origins}") String origenes) {
        this.handler = handler;
        this.origenesPermitidos = Arrays.stream(origenes.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Solo el frontend (o el mismo origen del gateway) puede abrir el socket: evita que otra página use la
        // cookie del usuario para escuchar sus avisos (cross-site WebSocket hijacking).
        registry.addHandler(handler, RUTA_WS).setAllowedOrigins(origenesPermitidos);
    }

    @Bean
    RedisMessageListenerContainer suscripcionNotificaciones(RedisConnectionFactory conexion, JsonMapper json) {
        RedisMessageListenerContainer contenedor = new RedisMessageListenerContainer();
        contenedor.setConnectionFactory(conexion);
        contenedor.addMessageListener((mensaje, patron) -> {
            String cuerpo = new String(mensaje.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            try {
                EventoNotificacion evento = json.readValue(cuerpo, EventoNotificacion.class);
                handler.entregar(evento.destinatarioId(), cuerpo);
            } catch (RuntimeException e) {
                log.warn("Se descartó un evento de notificación mal formado ({})", e.getClass().getSimpleName());
            }
        }, new ChannelTopic(EventoNotificacion.CANAL));
        return contenedor;
    }
}
