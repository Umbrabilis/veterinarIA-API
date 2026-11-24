package tech.veterinaria_api.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.auth.SesionDePrueba;
import tech.veterinaria_api.auth.SesionDePrueba.Cuenta;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.usuarios.dto.ActualizarPerfilRequest;
import tech.veterinaria_api.usuarios.dto.CambiarPasswordRequest;
import tech.veterinaria_api.usuarios.dto.VeterinarioResponse;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class UsuarioControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    private Cuenta registrar(String nombre, String email, RolUsuario rol) {
        return SesionDePrueba.registrarEIniciarSesion(restTestClient, nombre, email, rol);
    }

    @Test
    void publicaLaLlavePublicaComoJwksSinAutenticacion() {
        Map<String, Object> jwks = restTestClient.get().uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(jwks).containsKey("keys");
        List<?> llaves = (List<?>) jwks.get("keys");
        assertThat(llaves).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> llave = (Map<String, Object>) llaves.getFirst();
        assertThat(llave).containsKeys("kid", "n", "e").doesNotContainKey("d");
    }

    @Test
    void registraVeterinarioYEditaSuPerfil() {
        Cuenta registro = registrar("Laura Gómez", "laura@correo.test", RolUsuario.VETERINARIO);
        assertThat(registro.usuario().rol()).isEqualTo(RolUsuario.VETERINARIO);
        String bearer = "Bearer " + registro.token();

        UsuarioResponse actualizado = restTestClient.put().uri("/api/v1/usuarios/me")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ActualizarPerfilRequest("Laura Gómez Ruiz"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UsuarioResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualizado.nombre()).isEqualTo("Laura Gómez Ruiz");

        restTestClient.put().uri("/api/v1/usuarios/me/password")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CambiarPasswordRequest("incorrecta", "nuevaClave123"))
                .exchange()
                .expectStatus().isEqualTo(422);

        restTestClient.put().uri("/api/v1/usuarios/me/password")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CambiarPasswordRequest("password123", "nuevaClave123"))
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("laura@correo.test", "nuevaClave123"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void listaSoloVeterinariosActivos() {
        Cuenta vet = registrar("Dra. Marta Ríos", "marta.rios@clinica.test", RolUsuario.VETERINARIO);
        Cuenta admin = registrar("Admin Clínica", "admin.lista@clinica.test", RolUsuario.ADMINISTRADOR);

        List<VeterinarioResponse> veterinarios = restTestClient.get().uri("/api/v1/usuarios/veterinarios")
                .header("Authorization", "Bearer " + admin.token())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<VeterinarioResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(veterinarios).extracting(VeterinarioResponse::id).contains(vet.usuario().id())
                .doesNotContain(admin.usuario().id());

        restTestClient.get().uri("/api/v1/usuarios/veterinarios/{id}", admin.usuario().id())
                .header("Authorization", "Bearer " + admin.token())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void rechazaPerfilSinToken() {
        restTestClient.get().uri("/api/v1/usuarios/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
