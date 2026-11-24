package tech.veterinaria_api.notificaciones;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tech.veterinaria_api.security.UsuarioActual;

/**
 * Conexiones WebSocket abiertas en ESTA réplica. La autenticación ocurre antes, en el handshake HTTP: Spring Security
 * valida el JWT (cookie de sesión o header) y aquí solo se lee quién es. Cada réplica entrega los eventos que recibe
 * de Redis a sus propias conexiones, así que el servicio escala horizontalmente sin perder avisos.
 */
@Component
public class NotificacionesWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesWebSocketHandler.class);
    private static final int TIEMPO_ENVIO_MS = 5_000;
    private static final int BUFFER_BYTES = 64 * 1024;

    private final Map<String, Conexion> conexiones = new ConcurrentHashMap<>();

    private record Conexion(UUID usuarioId, WebSocketSession sesion) {
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) throws IOException {
        if (!(sesion.getPrincipal() instanceof JwtAuthenticationToken token)) {
            sesion.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        UsuarioActual usuario = UsuarioActual.de(token.getToken());
        if (!usuario.esPersonalClinico()) {
            sesion.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        conexiones.put(sesion.getId(), new Conexion(usuario.id(),
                new ConcurrentWebSocketSessionDecorator(sesion, TIEMPO_ENVIO_MS, BUFFER_BYTES)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        conexiones.remove(sesion.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) {
        // Canal de solo bajada: el cliente no envía nada (los "ping" del navegador se ignoran).
    }

    /** Envía el aviso a sus destinatarios conectados a esta réplica. */
    public void entregar(UUID destinatarioId, String mensajeJson) {
        TextMessage mensaje = new TextMessage(mensajeJson);
        conexiones.values().stream()
                .filter(c -> destinatarioId == null || destinatarioId.equals(c.usuarioId()))
                .forEach(c -> enviar(c, mensaje));
    }

    public int conexionesAbiertas() {
        return conexiones.size();
    }

    private void enviar(Conexion conexion, TextMessage mensaje) {
        try {
            if (conexion.sesion().isOpen()) {
                conexion.sesion().sendMessage(mensaje);
            }
        } catch (IOException | IllegalStateException e) {
            log.debug("No se pudo entregar un aviso por WebSocket ({})", e.getClass().getSimpleName());
        }
    }
}
