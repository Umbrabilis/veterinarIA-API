package tech.veterinaria_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.citas.EstadoCita;
import tech.veterinaria_api.citas.dto.CambiarEstadoCitaRequest;
import tech.veterinaria_api.citas.dto.CitaResponse;
import tech.veterinaria_api.citas.dto.CrearCitaRequest;
import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.remoto.UsuariosClient;
import tech.veterinaria_api.remoto.UsuariosClient.VeterinarioRemoto;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

/**
 * Los otros microservicios se simulan (son HTTP externos a este servicio); el SQL de la agenda, incluida la
 * restricción EXCLUDE de no solapamiento, corre contra PostgreSQL real.
 */
@Import({ TestcontainersConfiguration.class, JwtDePruebaConfiguration.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CitaControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TokensDePrueba tokens;

    @MockitoBean
    private PacientesClient pacientesClient;

    @MockitoBean
    private UsuariosClient usuariosClient;

    private final UUID veterinarioId = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();
    private final UUID mascotaId = UUID.randomUUID();
    private final UUID usuarioPropietarioId = UUID.randomUUID();

    @BeforeEach
    void configurarServiciosRemotos() {
        given(usuariosClient.obtenerVeterinario(veterinarioId))
                .willReturn(new VeterinarioRemoto(veterinarioId, "Dra. Marta Ríos"));
        given(pacientesClient.obtenerMascota(mascotaId))
                .willReturn(new MascotaRemota(mascotaId, propietarioId, "Luna", true));
    }

    private String vet() {
        return tokens.bearer(veterinarioId, "vet@clinica.test", RolUsuario.VETERINARIO);
    }

    private String duena() {
        return tokens.bearer(usuarioPropietarioId, "duena@correo.test", RolUsuario.PROPIETARIO);
    }

    private CrearCitaRequest solicitud(Instant inicio) {
        return new CrearCitaRequest(mascotaId, veterinarioId, inicio, inicio.plus(30, ChronoUnit.MINUTES),
                "Control de vacunas");
    }

    private Instant manana(int hora) {
        return Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS).plus(hora, ChronoUnit.MINUTES);
    }

    @Test
    void agendaYRechazaSolapamientoDelMismoVeterinario() {
        Instant inicio = manana(0);
        CitaResponse cita = restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud(inicio))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CitaResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(cita.estado()).isEqualTo(EstadoCita.PROGRAMADA);
        assertThat(cita.propietarioId()).isEqualTo(propietarioId);
        assertThat(cita.mascotaNombre()).isEqualTo("Luna");

        restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud(inicio.plus(15, ChronoUnit.MINUTES)))
                .exchange()
                .expectStatus().isEqualTo(409);

        // Una cita cancelada libera el horario (la restricción solo aplica a citas activas).
        restTestClient.patch().uri("/api/v1/citas/{id}/estado", cita.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CambiarEstadoCitaRequest(EstadoCita.CANCELADA))
                .exchange()
                .expectStatus().isOk();

        restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud(inicio.plus(15, ChronoUnit.MINUTES)))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void elPropietarioAgendaVeYCancelaSoloSusCitas() {
        given(pacientesClient.miPropietarioId()).willReturn(Optional.of(propietarioId));

        CitaResponse cita = restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", duena())
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud(manana(120)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CitaResponse.class)
                .returnResult()
                .getResponseBody();

        List<CitaResponse> mias = restTestClient.get().uri("/api/v1/citas/mias")
                .header("Authorization", duena())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<CitaResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(mias).extracting(CitaResponse::id).contains(cita.id());

        // Otro propietario no puede ver ni cancelar la cita cambiando el id en la URL.
        UUID otroUsuario = UUID.randomUUID();
        String otro = tokens.bearer(otroUsuario, "otro@correo.test", RolUsuario.PROPIETARIO);
        given(pacientesClient.miPropietarioId()).willReturn(Optional.of(UUID.randomUUID()));
        restTestClient.get().uri("/api/v1/citas/{id}", cita.id())
                .header("Authorization", otro)
                .exchange()
                .expectStatus().isForbidden();
        restTestClient.patch().uri("/api/v1/citas/{id}/cancelar", cita.id())
                .header("Authorization", otro)
                .exchange()
                .expectStatus().isForbidden();

        given(pacientesClient.miPropietarioId()).willReturn(Optional.of(propietarioId));
        CitaResponse cancelada = restTestClient.patch().uri("/api/v1/citas/{id}/cancelar", cita.id())
                .header("Authorization", duena())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CitaResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(cancelada.estado()).isEqualTo(EstadoCita.CANCELADA);

        // El propietario no puede cambiar estados administrativos.
        restTestClient.patch().uri("/api/v1/citas/{id}/estado", cita.id())
                .header("Authorization", duena())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CambiarEstadoCitaRequest(EstadoCita.ATENDIDA))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void propietarioNoPuedeAgendarParaMascotaAjena() {
        UUID mascotaAjena = UUID.randomUUID();
        given(pacientesClient.obtenerMascota(any())).willThrow(new AccesoDenegadoException());

        restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", duena())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearCitaRequest(mascotaAjena, veterinarioId, manana(240),
                        manana(270), "Consulta"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void rechazaHorarioInvalido() {
        Instant inicio = manana(360);
        restTestClient.post().uri("/api/v1/citas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearCitaRequest(mascotaId, veterinarioId, inicio, inicio.minus(5, ChronoUnit.MINUTES),
                        "Consulta"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }
}
