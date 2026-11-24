package tech.veterinaria_api.notificaciones;

import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.security.UsuarioActual;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Avisos para el personal. En tiempo real: WebSocket en /api/v1/notificaciones/ws")
public class NotificacionController {

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    @GetMapping("/recientes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','VETERINARIO')")
    @Operation(summary = "Últimos avisos para el usuario (los suyos y los de toda la clínica), del más nuevo al más viejo")
    public List<EventoNotificacion> recientes(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limite) {
        UsuarioActual usuario = UsuarioActual.de(jwt);
        List<String> guardados = redis.opsForList().range(EventoNotificacion.HISTORIAL, 0, -1);
        if (guardados == null) {
            return List.of();
        }
        return guardados.stream()
                .map(this::leer)
                .filter(Objects::nonNull)
                .filter(e -> e.destinatarioId() == null || e.destinatarioId().equals(usuario.id()))
                .limit(limite)
                .toList();
    }

    private EventoNotificacion leer(String texto) {
        try {
            return json.readValue(texto, EventoNotificacion.class);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
