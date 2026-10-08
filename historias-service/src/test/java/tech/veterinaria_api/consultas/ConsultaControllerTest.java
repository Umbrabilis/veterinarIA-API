package tech.veterinaria_api.consultas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.common.AccesoDenegadoException;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.consultas.dto.ConsultaResponse;
import tech.veterinaria_api.consultas.dto.CrearConsultaRequest;
import tech.veterinaria_api.consultas.dto.DatosClinicosRequest;
import tech.veterinaria_api.consultas.dto.EnmiendaRequest;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.seguimiento.SeguimientoItem;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

@Import({ TestcontainersConfiguration.class, JwtDePruebaConfiguration.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ConsultaControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TokensDePrueba tokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PacientesClient pacientesClient;

    private final UUID veterinarioId = UUID.randomUUID();
    private final UUID mascotaId = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();

    @BeforeEach
    void configurarPacientes() {
        given(pacientesClient.obtenerMascota(mascotaId)).willReturn(new MascotaRemota(mascotaId, propietarioId,
                "Luna", "PERRO", "Criollo", "HEMBRA", LocalDate.now().minusYears(4), new BigDecimal("11.20"), true));
    }

    private String vet() {
        return tokens.bearer(veterinarioId, "vet@clinica.test", RolUsuario.VETERINARIO);
    }

    private DatosClinicosRequest datos(String diagnostico) {
        return new DatosClinicosRequest("Vómito desde ayer", "Vómito, decaimiento", "Abdomen sensible",
                new BigDecimal("11.20"), new BigDecimal("39.1"), diagnostico, "Omeprazol 1 mg/kg cada 24 h por 5 días",
                "Dieta blanda y agua fresca", LocalDate.now().plusDays(7));
    }

    private ConsultaResponse abrirConsulta(String diagnostico) {
        return restTestClient.post().uri("/api/v1/consultas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearConsultaRequest(mascotaId, null, datos(diagnostico)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ConsultaResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ConsultaResponse cerrar(UUID consultaId) {
        return restTestClient.post().uri("/api/v1/consultas/{id}/cerrar", consultaId)
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsultaResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void unaConsultaCerradaNoSeEditaSeEnmienda() {
        ConsultaResponse consulta = abrirConsulta("Gastritis aguda");
        assertThat(consulta.estado()).isEqualTo(EstadoConsulta.ABIERTA);
        assertThat(consulta.propietarioId()).isEqualTo(propietarioId);

        ConsultaResponse cerrada = cerrar(consulta.id());
        assertThat(cerrada.estado()).isEqualTo(EstadoConsulta.CERRADA);
        assertThat(cerrada.cerradaEn()).isNotNull();

        restTestClient.put().uri("/api/v1/consultas/{id}", consulta.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(datos("Otro diagnóstico"))
                .exchange()
                .expectStatus().isEqualTo(422);

        restTestClient.post().uri("/api/v1/consultas/{id}/enmiendas", consulta.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EnmiendaRequest("Error de dosis", "La dosis correcta de omeprazol es 0.7 mg/kg"))
                .exchange()
                .expectStatus().isCreated();

        ConsultaResponse detalle = restTestClient.get().uri("/api/v1/consultas/{id}", consulta.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsultaResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(detalle.diagnostico()).isEqualTo("Gastritis aguda");
        assertThat(detalle.enmiendas()).singleElement()
                .satisfies(e -> assertThat(e.autorId()).isEqualTo(veterinarioId));
    }

    @Test
    void laBaseDeDatosImpideModificarOBorrarUnaConsultaCerrada() {
        ConsultaResponse consulta = abrirConsulta("Otitis externa");
        cerrar(consulta.id());

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE consultas SET diagnostico = 'alterado' WHERE id = ?",
                consulta.id())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM consultas WHERE id = ?", consulta.id()))
                .isInstanceOf(DataAccessException.class);
        restTestClient.post().uri("/api/v1/consultas/{id}/enmiendas", consulta.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EnmiendaRequest("Aclaración", "Oído izquierdo"))
                .exchange()
                .expectStatus().isCreated();
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE enmiendas SET contenido = 'x' WHERE consulta_id = ?",
                consulta.id())).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.execute("TRUNCATE consultas CASCADE"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.execute("TRUNCATE vacunas"))
                .isInstanceOf(DataAccessException.class);

        String diagnostico = jdbcTemplate.queryForObject("SELECT diagnostico FROM consultas WHERE id = ?",
                String.class, consulta.id());
        assertThat(diagnostico).isEqualTo("Otitis externa");
    }

    @Test
    void noSeCierraSinDiagnosticoYSoloLaEditaSuVeterinario() {
        ConsultaResponse consulta = abrirConsulta(null);

        restTestClient.post().uri("/api/v1/consultas/{id}/cerrar", consulta.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isEqualTo(422);

        String otroVet = tokens.bearer(UUID.randomUUID(), "otro.vet@clinica.test", RolUsuario.VETERINARIO);
        restTestClient.put().uri("/api/v1/consultas/{id}", consulta.id())
                .header("Authorization", otroVet)
                .contentType(MediaType.APPLICATION_JSON)
                .body(datos("Diagnóstico ajeno"))
                .exchange()
                .expectStatus().isForbidden();

        String admin = tokens.bearer(UUID.randomUUID(), "admin@clinica.test", RolUsuario.ADMINISTRADOR);
        restTestClient.post().uri("/api/v1/consultas")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearConsultaRequest(mascotaId, null, datos("x")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void elPropietarioVeSoloConsultasCerradasDeSusMascotas() {
        ConsultaResponse abierta = abrirConsulta("Dermatitis");
        ConsultaResponse cerrada = abrirConsulta("Conjuntivitis");
        cerrar(cerrada.id());

        UUID usuarioDueno = UUID.randomUUID();
        String dueno = tokens.bearer(usuarioDueno, "dueno@correo.test", RolUsuario.PROPIETARIO);

        List<ConsultaResponse> historia = restTestClient.get().uri("/api/v1/consultas?mascotaId={id}", mascotaId)
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsultaResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(historia).extracting(ConsultaResponse::id).contains(cerrada.id()).doesNotContain(abierta.id());

        restTestClient.get().uri("/api/v1/consultas/{id}", abierta.id())
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.get().uri("/api/v1/consultas/{id}", cerrada.id())
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isOk();

        // Si la mascota no es suya (otro dueño, o cambió de dueño) no ve la consulta aunque conozca el id.
        willThrow(new AccesoDenegadoException()).given(pacientesClient).obtenerMascota(any());
        restTestClient.get().uri("/api/v1/consultas/{id}", cerrada.id())
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isForbidden();
        restTestClient.get().uri("/api/v1/consultas?mascotaId={id}", mascotaId)
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void elSeguimientoListaControlesYRefuerzosPendientes() {
        ConsultaResponse consulta = abrirConsulta("Control postoperatorio");
        cerrar(consulta.id());

        restTestClient.post().uri("/api/v1/vacunas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new tech.veterinaria_api.vacunas.dto.VacunaRequest(mascotaId, "Rabia", "L-123",
                        LocalDate.now().minusYears(1).plusDays(10), LocalDate.now().plusDays(10), null))
                .exchange()
                .expectStatus().isCreated();

        List<SeguimientoItem> pendientes = restTestClient.get().uri("/api/v1/seguimiento/pendientes?dias=30")
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<SeguimientoItem>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(pendientes).filteredOn(i -> i.mascotaId().equals(mascotaId))
                .extracting(SeguimientoItem::tipo)
                .containsExactlyInAnyOrder(SeguimientoItem.TipoSeguimiento.CONTROL,
                        SeguimientoItem.TipoSeguimiento.REFUERZO_VACUNA);
    }
}
