package tech.veterinaria_api.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.usuarios.dto.ActualizarUsuarioRequest;
import tech.veterinaria_api.usuarios.dto.CrearUsuarioRequest;
import tech.veterinaria_api.usuarios.dto.UsuarioAdminResponse;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GestionUsuariosTest {

    @Autowired
    private RestTestClient restTestClient;

    private AuthResponse registrar(RolUsuario rol) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@clinica.test";
        return restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("Persona Prueba", email, "password123", rol))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private String bearer(AuthResponse cuenta) {
        return "Bearer " + cuenta.accessToken();
    }

    private UsuarioAdminResponse crear(String bearerAdmin, String email, RolUsuario rol) {
        return restTestClient.post().uri("/api/v1/usuarios")
                .header("Authorization", bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearUsuarioRequest("Diana Torres", email, "clave1234", rol))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UsuarioAdminResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void elAdministradorCreaYListaAlPersonal() {
        String admin = bearer(registrar(RolUsuario.ADMINISTRADOR));
        String email = "diana-" + UUID.randomUUID() + "@clinica.test";

        UsuarioAdminResponse creado = crear(admin, email, RolUsuario.VETERINARIO);
        assertThat(creado.activo()).isTrue();
        assertThat(creado.rol()).isEqualTo(RolUsuario.VETERINARIO);

        List<UsuarioAdminResponse> personal = restTestClient.get().uri("/api/v1/usuarios")
                .header("Authorization", admin)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<UsuarioAdminResponse>>() {
                })
                .returnResult()
                .getResponseBody();
        assertThat(personal).extracting(UsuarioAdminResponse::email).contains(email);
        assertThat(personal).extracting(UsuarioAdminResponse::rol).doesNotContain(RolUsuario.PROPIETARIO);

        // La cuenta creada puede iniciar sesión con la contraseña inicial.
        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "clave1234"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unVeterinarioNoPuedeVerNiGestionarUsuarios() {
        String veterinario = bearer(registrar(RolUsuario.VETERINARIO));

        restTestClient.get().uri("/api/v1/usuarios")
                .header("Authorization", veterinario)
                .exchange()
                .expectStatus().isForbidden();

        restTestClient.post().uri("/api/v1/usuarios")
                .header("Authorization", veterinario)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearUsuarioRequest("Intruso", "intruso-" + UUID.randomUUID() + "@clinica.test",
                        "clave1234", RolUsuario.ADMINISTRADOR))
                .exchange()
                .expectStatus().isForbidden();

        // Sigue pudiendo ver la lista mínima de veterinarios (solo id y nombre) para agendar citas.
        restTestClient.get().uri("/api/v1/usuarios/veterinarios")
                .header("Authorization", veterinario)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void desactivarUnaCuentaLeImpideIniciarSesion() {
        String admin = bearer(registrar(RolUsuario.ADMINISTRADOR));
        String email = "baja-" + UUID.randomUUID() + "@clinica.test";
        UsuarioAdminResponse creado = crear(admin, email, RolUsuario.VETERINARIO);

        UsuarioAdminResponse desactivado = restTestClient.put().uri("/api/v1/usuarios/{id}", creado.id())
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ActualizarUsuarioRequest("Diana Torres", RolUsuario.VETERINARIO, false))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UsuarioAdminResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(desactivado.activo()).isFalse();

        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "clave1234"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void elAdministradorNoPuedeQuitarseElRolNiDesactivarse() {
        AuthResponse admin = registrar(RolUsuario.ADMINISTRADOR);
        UUID miId = admin.usuario().id();

        for (ActualizarUsuarioRequest cambio : List.of(
                new ActualizarUsuarioRequest("Persona Prueba", RolUsuario.VETERINARIO, true),
                new ActualizarUsuarioRequest("Persona Prueba", RolUsuario.ADMINISTRADOR, false))) {
            restTestClient.put().uri("/api/v1/usuarios/{id}", miId)
                    .header("Authorization", bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cambio)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    @Test
    void rechazaCorreoRepetidoYCuentasDePropietario() {
        String admin = bearer(registrar(RolUsuario.ADMINISTRADOR));
        String email = "repetido-" + UUID.randomUUID() + "@clinica.test";
        crear(admin, email, RolUsuario.VETERINARIO);

        restTestClient.post().uri("/api/v1/usuarios")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearUsuarioRequest("Otra Persona", email.toUpperCase(), "clave1234", RolUsuario.VETERINARIO))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.post().uri("/api/v1/usuarios")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CrearUsuarioRequest("Dueña", "duena-" + UUID.randomUUID() + "@clinica.test", "clave1234",
                        RolUsuario.PROPIETARIO))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
