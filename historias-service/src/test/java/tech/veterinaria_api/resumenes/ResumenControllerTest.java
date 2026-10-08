package tech.veterinaria_api.resumenes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.consultas.dto.ConsultaResponse;
import tech.veterinaria_api.consultas.dto.CrearConsultaRequest;
import tech.veterinaria_api.consultas.dto.DatosClinicosRequest;
import tech.veterinaria_api.remoto.PacientesClient;
import tech.veterinaria_api.remoto.PacientesClient.MascotaRemota;
import tech.veterinaria_api.remoto.PacientesClient.PropietarioRemoto;
import tech.veterinaria_api.resumenes.dto.AprobarResumenRequest;
import tech.veterinaria_api.resumenes.dto.ResumenResponse;
import tech.veterinaria_api.resumenes.ia.PlantillaModeloIA;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

@Import({ TestcontainersConfiguration.class, JwtDePruebaConfiguration.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ResumenControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TokensDePrueba tokens;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PacientesClient pacientesClient;

    @MockitoBean
    private NotificadorResumen notificador;

    private final UUID veterinarioId = UUID.randomUUID();
    private final UUID mascotaId = UUID.randomUUID();
    private final UUID propietarioId = UUID.randomUUID();

    @BeforeEach
    void configurarPacientes() {
        given(pacientesClient.obtenerMascota(mascotaId)).willReturn(new MascotaRemota(mascotaId, propietarioId,
                "Luna", "GATO", "Siamés", "HEMBRA", LocalDate.now().minusYears(2), new BigDecimal("4.10"), true));
        given(pacientesClient.obtenerPropietario(propietarioId))
                .willReturn(new PropietarioRemoto(propietarioId, "Carlos Pérez", "carlos@correo.test"));
    }

    private String vet() {
        return tokens.bearer(veterinarioId, "vet@clinica.test", RolUsuario.VETERINARIO);
    }

    private ResumenResponse cerrarConsultaYObtenerBorrador() {
        ConsultaResponse consulta = restTestClient.post().uri("/api/v1/consultas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearConsultaRequest(mascotaId, null, new DatosClinicosRequest("Estornudos",
                        "Estornudos y secreción nasal", "Mucosas rosadas", null, new BigDecimal("38.9"),
                        "Rinitis viral leve", "Nebulizaciones con suero fisiológico 2 veces al día por 5 días",
                        "Mantenerla en un lugar sin corrientes de aire", LocalDate.now().plusDays(10))))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ConsultaResponse.class)
                .returnResult()
                .getResponseBody();

        restTestClient.post().uri("/api/v1/consultas/{id}/cerrar", consulta.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk();

        return restTestClient.get().uri("/api/v1/consultas/{id}/resumen", consulta.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResumenResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void alCerrarLaConsultaSeGeneraUnBorradorConTrazabilidad() {
        ResumenResponse borrador = cerrarConsultaYObtenerBorrador();

        assertThat(borrador.estado()).isEqualTo(EstadoResumen.BORRADOR);
        assertThat(borrador.modelo()).isEqualTo(PlantillaModeloIA.MODELO);
        assertThat(borrador.versionPrompt()).isEqualTo(PlantillaModeloIA.VERSION);
        assertThat(borrador.generado().hallazgos()).contains("Rinitis viral leve");
        assertThat(borrador.generado().tratamiento()).contains("2 veces al día");
        assertThat(borrador.contenidoFinal()).isNull();
        // Ningún dato personal del propietario en el contenido generado.
        assertThat(borrador.generado().toString()).doesNotContain("Carlos", "carlos@correo.test");
    }

    @Test
    void noSeEnviaNingunResumenSinAprobacionDelVeterinario() {
        ResumenResponse borrador = cerrarConsultaYObtenerBorrador();

        restTestClient.post().uri("/api/v1/resumenes/{id}/enviar", borrador.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isEqualTo(422);
        verify(notificador, never()).enviar(anyString(), anyString(), anyString(), any());

        // Ni siquiera escribiendo directo en la base de datos.
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE resumenes_consulta SET estado = 'ENVIADO', enviado_en = now() WHERE id = ?", borrador.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE resumenes_consulta SET estado = 'APROBADO' WHERE id = ?", borrador.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE resumenes_consulta SET generado_hallazgos = 'otro' WHERE id = ?", borrador.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.execute("TRUNCATE resumenes_consulta"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void elVeterinarioEditaApruebaYEnviaYElDuenoLoVeSoloAlFinal() {
        ResumenResponse borrador = cerrarConsultaYObtenerBorrador();
        String dueno = tokens.bearer(UUID.randomUUID(), "carlos@correo.test", RolUsuario.PROPIETARIO);

        restTestClient.get().uri("/api/v1/resumenes/{id}", borrador.id())
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isForbidden();

        ContenidoResumen editado = new ContenidoResumen("Luna tiene una rinitis viral leve (como un resfriado).",
                "Nebulizaciones con suero fisiológico 2 veces al día por 5 días.",
                "Evita corrientes de aire. Si deja de comer o respira con dificultad, vuelve antes.",
                "Control en 10 días.");

        // Otro veterinario no puede aprobar el resumen de una consulta que no atendió.
        String otroVet = tokens.bearer(UUID.randomUUID(), "otro@clinica.test", RolUsuario.VETERINARIO);
        restTestClient.post().uri("/api/v1/resumenes/{id}/aprobar", borrador.id())
                .header("Authorization", otroVet)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AprobarResumenRequest(null))
                .exchange()
                .expectStatus().isForbidden();

        ResumenResponse aprobado = restTestClient.post().uri("/api/v1/resumenes/{id}/aprobar", borrador.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AprobarResumenRequest(editado))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResumenResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(aprobado.estado()).isEqualTo(EstadoResumen.APROBADO);
        assertThat(aprobado.aprobadoPor()).isEqualTo(veterinarioId);
        assertThat(aprobado.contenidoFinal()).isEqualTo(editado);
        assertThat(aprobado.generado()).isEqualTo(borrador.generado());

        restTestClient.put().uri("/api/v1/resumenes/{id}", borrador.id())
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(editado)
                .exchange()
                .expectStatus().isEqualTo(422);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE resumenes_consulta SET final_hallazgos = 'cambiado' WHERE id = ?", borrador.id()))
                .isInstanceOf(DataAccessException.class);

        ResumenResponse enviado = restTestClient.post().uri("/api/v1/resumenes/{id}/enviar", borrador.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResumenResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(enviado.estado()).isEqualTo(EstadoResumen.ENVIADO);
        verify(notificador).enviar(eq("carlos@correo.test"), eq("Carlos Pérez"), eq("Luna"), eq(editado));

        ResumenResponse vistaDueno = restTestClient.get().uri("/api/v1/resumenes/{id}", borrador.id())
                .header("Authorization", dueno)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ResumenResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(vistaDueno.contenidoFinal()).isEqualTo(editado);
        assertThat(vistaDueno.generado()).isNull();
        assertThat(vistaDueno.modelo()).isNull();
    }
}
