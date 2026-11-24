package tech.veterinaria_api.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

/** Redis real (Testcontainers) y un cliente WebSocket real contra el servidor levantado en un puerto aleatorio. */
@Import({ JwtDePruebaConfiguration.class, NotificacionesTest.RedisDePrueba.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class NotificacionesTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class RedisDePrueba {
        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        }
    }

    @LocalServerPort
    private int puerto;

    @Autowired
    private TokensDePrueba tokens;

    @Autowired
    private PublicadorNotificaciones publicador;

    @Autowired
    private RestTestClient restTestClient;

    private record Cliente(WebSocketSession sesion, BlockingQueue<String> recibidos) {
    }

    private Cliente conectar(String bearer) throws Exception {
        BlockingQueue<String> recibidos = new LinkedBlockingQueue<>();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, bearer);
        headers.setOrigin("http://localhost:5173");
        WebSocketSession sesion = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage mensaje) {
                recibidos.add(mensaje.getPayload());
            }
        }, headers, URI.create("ws://localhost:" + puerto + NotificacionesConfig.RUTA_WS)).get(5, TimeUnit.SECONDS);
        return new Cliente(sesion, recibidos);
    }

    @Test
    void entregaEnTiempoRealSoloASusDestinatarios() throws Exception {
        UUID vetA = UUID.randomUUID();
        UUID vetB = UUID.randomUUID();
        Cliente a = conectar(tokens.bearer(vetA, "a@clinica.test", RolUsuario.VETERINARIO));
        Cliente b = conectar(tokens.bearer(vetB, "b@clinica.test", RolUsuario.VETERINARIO));

        publicador.publicar(EventoNotificacion.para(vetA, TipoNotificacion.RESUMEN_PENDIENTE,
                "Resumen listo para revisar", "El resumen de la consulta de Luna está listo", UUID.randomUUID()));
        String paraA = a.recibidos().poll(5, TimeUnit.SECONDS);
        assertThat(paraA).contains("RESUMEN_PENDIENTE", "Luna");
        assertThat(b.recibidos().poll(1, TimeUnit.SECONDS)).as("vetB no es el destinatario").isNull();

        publicador.publicar(EventoNotificacion.paraTodos(TipoNotificacion.CITA_AGENDADA, "Nueva cita",
                "Rocky con Dra. Ríos", UUID.randomUUID()));
        assertThat(a.recibidos().poll(5, TimeUnit.SECONDS)).contains("CITA_AGENDADA");
        assertThat(b.recibidos().poll(5, TimeUnit.SECONDS)).contains("CITA_AGENDADA");

        a.sesion().close();
        b.sesion().close();
    }

    @Test
    void elHistorialMuestraLosAvisosDelUsuarioYLosGenerales() {
        UUID vet = UUID.randomUUID();
        publicador.publicar(EventoNotificacion.para(vet, TipoNotificacion.RESUMEN_PENDIENTE, "Para mí", "m", null));
        publicador.publicar(EventoNotificacion.para(UUID.randomUUID(), TipoNotificacion.RESUMEN_PENDIENTE,
                "Para otro", "m", null));
        publicador.publicar(EventoNotificacion.paraTodos(TipoNotificacion.CITA_CANCELADA, "Para todos", "m", null));

        List<EventoNotificacion> recientes = restTestClient.get().uri("/api/v1/notificaciones/recientes")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(vet, "vet@clinica.test", RolUsuario.VETERINARIO))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<EventoNotificacion>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(recientes).extracting(EventoNotificacion::titulo)
                .contains("Para mí", "Para todos").doesNotContain("Para otro");
        assertThat(recientes.getFirst().titulo()).isEqualTo("Para todos");
    }

    @Test
    void sinSesionNoSeAbreElSocket() {
        assertThatThrownBy(() -> new StandardWebSocketClient().execute(new TextWebSocketHandler(),
                URI.create("ws://localhost:" + puerto + NotificacionesConfig.RUTA_WS).toString())
                .get(5, TimeUnit.SECONDS)).hasMessageContaining("401");
    }

    @Test
    void otroOrigenNoPuedeAbrirElSocket() {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID(), "v@clinica.test", RolUsuario.VETERINARIO));
        headers.setOrigin("https://sitio-malicioso.test");
        assertThatThrownBy(() -> new StandardWebSocketClient().execute(new TextWebSocketHandler(), headers,
                URI.create("ws://localhost:" + puerto + NotificacionesConfig.RUTA_WS)).get(5, TimeUnit.SECONDS))
                .hasMessageContaining("403");
    }
}
