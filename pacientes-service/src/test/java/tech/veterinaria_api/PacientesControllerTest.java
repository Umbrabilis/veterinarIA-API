package tech.veterinaria_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.common.ApiError;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.mascotas.Especie;
import tech.veterinaria_api.mascotas.Sexo;
import tech.veterinaria_api.mascotas.dto.MascotaRequest;
import tech.veterinaria_api.mascotas.dto.MascotaResponse;
import tech.veterinaria_api.propietarios.dto.ActualizarMisDatosRequest;
import tech.veterinaria_api.propietarios.dto.CodigoVinculacionResponse;
import tech.veterinaria_api.propietarios.dto.PropietarioRequest;
import tech.veterinaria_api.propietarios.dto.PropietarioResponse;
import tech.veterinaria_api.propietarios.dto.VincularCuentaRequest;
import tech.veterinaria_api.testing.JwtDePruebaConfiguration;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.testing.TokensDePrueba;

@Import({ TestcontainersConfiguration.class, JwtDePruebaConfiguration.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class PacientesControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private TokensDePrueba tokens;

    private String vet() {
        return tokens.bearer(UUID.randomUUID(), "vet@clinica.test", RolUsuario.VETERINARIO);
    }

    private PropietarioResponse crearPropietario(String documento, String email) {
        return restTestClient.post().uri("/api/v1/propietarios")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropietarioRequest("Carlos Pérez", documento, "3001234567", email, "Calle 10 # 20-30",
                        true))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PropietarioResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private MascotaResponse crearMascota(UUID propietarioId, String nombre) {
        return restTestClient.post().uri("/api/v1/mascotas")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MascotaRequest(propietarioId, nombre, Especie.PERRO, "Criollo", Sexo.MACHO,
                        LocalDate.now().minusYears(3), new BigDecimal("12.50"), "Café", null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MascotaResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void elPersonalRegistraPropietarioYMascota() {
        PropietarioResponse propietario = crearPropietario("1001", "carlos@correo.test");
        assertThat(propietario.consentimientoDatosEn()).isNotNull();

        MascotaResponse mascota = crearMascota(propietario.id(), "Toby");
        assertThat(mascota.propietarioId()).isEqualTo(propietario.id());
        assertThat(mascota.activo()).isTrue();

        restTestClient.get().uri("/api/v1/mascotas/{id}", mascota.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void rechazaPropietarioSinAutorizacionDeDatosYDocumentoDuplicado() {
        restTestClient.post().uri("/api/v1/propietarios")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropietarioRequest("Ana", "2002", "3001234567", null, null, false))
                .exchange()
                .expectStatus().isBadRequest();

        crearPropietario("2003", null);
        ApiError error = restTestClient.post().uri("/api/v1/propietarios")
                .header("Authorization", vet())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PropietarioRequest("Otro", "2003", "3001234567", null, null, true))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();
        assertThat(error.message()).doesNotContain("2003");
    }

    @Test
    void registrarseConElEmailDeOtroNoDaAccesoASusDatos() {
        crearPropietario("4001", "victima@correo.test");

        // Cuenta nueva con el email de la víctima: sin código de la clínica no queda vinculada a nada.
        String atacante = tokens.bearer(UUID.randomUUID(), "victima@correo.test", RolUsuario.PROPIETARIO);
        restTestClient.get().uri("/api/v1/propietarios/me")
                .header("Authorization", atacante)
                .exchange()
                .expectStatus().isNotFound();
        restTestClient.post().uri("/api/v1/propietarios/me/vincular")
                .header("Authorization", atacante)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VincularCuentaRequest("ABCDEFGHJK"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void elPropietarioVinculaSuCuentaConCodigoYVeSoloSusMascotas() {
        PropietarioResponse propio = crearPropietario("3001", "duena@correo.test");
        PropietarioResponse ajeno = crearPropietario("3002", "otro@correo.test");
        MascotaResponse miMascota = crearMascota(propio.id(), "Luna");
        MascotaResponse mascotaAjena = crearMascota(ajeno.id(), "Rocky");

        CodigoVinculacionResponse codigo = restTestClient.post()
                .uri("/api/v1/propietarios/{id}/codigo-vinculacion", propio.id())
                .header("Authorization", vet())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CodigoVinculacionResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(codigo.codigo()).hasSize(10);

        UUID usuarioId = UUID.randomUUID();
        String duena = tokens.bearer(usuarioId, "duena@correo.test", RolUsuario.PROPIETARIO);

        PropietarioResponse yo = restTestClient.post().uri("/api/v1/propietarios/me/vincular")
                .header("Authorization", duena)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VincularCuentaRequest(codigo.codigo().toLowerCase()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropietarioResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(yo.id()).isEqualTo(propio.id());
        assertThat(yo.usuarioId()).isEqualTo(usuarioId);

        // El código es de un solo uso.
        restTestClient.post().uri("/api/v1/propietarios/me/vincular")
                .header("Authorization", tokens.bearer(UUID.randomUUID(), "x@correo.test", RolUsuario.PROPIETARIO))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VincularCuentaRequest(codigo.codigo()))
                .exchange()
                .expectStatus().isEqualTo(422);

        restTestClient.get().uri("/api/v1/propietarios/me")
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isOk();

        List<MascotaResponse> mias = restTestClient.get().uri("/api/v1/mascotas/mias")
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<MascotaResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(mias).extracting(MascotaResponse::id).containsExactly(miMascota.id());

        restTestClient.get().uri("/api/v1/mascotas/{id}", miMascota.id())
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isOk();

        // Autorización por pertenencia: cambiar el id en la URL no da acceso a la mascota de otro.
        restTestClient.get().uri("/api/v1/mascotas/{id}", mascotaAjena.id())
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isForbidden();
        restTestClient.get().uri("/api/v1/propietarios/{id}", ajeno.id())
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isForbidden();

        // Rutas solo para el personal de la clínica.
        restTestClient.get().uri("/api/v1/propietarios")
                .header("Authorization", duena)
                .exchange()
                .expectStatus().isForbidden();

        PropietarioResponse actualizado = restTestClient.put().uri("/api/v1/propietarios/me")
                .header("Authorization", duena)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ActualizarMisDatosRequest("3109876543", "Carrera 5 # 1-2"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PropietarioResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualizado.telefono()).isEqualTo("3109876543");
    }

    @Test
    void propietarioSinRegistroEnLaClinicaRecibe404() {
        restTestClient.get().uri("/api/v1/propietarios/me")
                .header("Authorization", tokens.bearer(UUID.randomUUID(), "nuevo@correo.test", RolUsuario.PROPIETARIO))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void sinTokenNoHayAcceso() {
        restTestClient.get().uri("/api/v1/mascotas")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
