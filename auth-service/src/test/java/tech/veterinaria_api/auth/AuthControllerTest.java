package tech.veterinaria_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.ApiError;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.usuarios.UsuarioService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AuthControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registraLogueaYConsultaElUsuarioAutenticado() {
        RegisterRequest registro = new RegisterRequest("Ada Lovelace", "ada@veterinaria.tech", "password123",
                RolUsuario.VETERINARIO);

        AuthResponse registroResponse = restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(registroResponse).isNotNull();
        assertThat(registroResponse.accessToken()).isNotBlank();
        assertThat(registroResponse.usuario().email()).isEqualTo("ada@veterinaria.tech");
        assertThat(registroResponse.usuario().rol()).isEqualTo(RolUsuario.VETERINARIO);

        LoginRequest login = new LoginRequest("ada@veterinaria.tech", "password123");
        AuthResponse loginResponse = restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(login)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(loginResponse).isNotNull();
        String token = loginResponse.accessToken();
        assertThat(token).isNotBlank();

        UsuarioResponse me = restTestClient.get().uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UsuarioResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(me).isNotNull();
        assertThat(me.email()).isEqualTo("ada@veterinaria.tech");
    }

    @Test
    void rechazaRegistroConEmailDuplicado() {
        RegisterRequest registro = new RegisterRequest("Grace Hopper", "grace@veterinaria.tech", "password123",
                RolUsuario.ADMINISTRADOR);

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isCreated();

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiError.class);
    }

    @Test
    void rechazaLoginConCredencialesInvalidas() {
        LoginRequest login = new LoginRequest("no-existe@veterinaria.tech", "loquesea123");

        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(login)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiError.class);
    }

    @ParameterizedTest(name = "{3}")
    @CsvSource(delimiter = '|', value = {
            "<img src=x onerror=alert(1)> | html@veterinaria.tech   | password123 | nombre con HTML",
            "@@@###                        | simbolos@veterinaria.tech | password123 | nombre solo con símbolos",
            "Ana Gómez                     | ana@dominio               | password123 | correo sin extensión",
            "Ana Gómez                     | ana gomez@veterinaria.tech | password123 | correo con espacio",
            "Ana Gómez                     | ana2@veterinaria.tech     | 12345678    | contraseña sin letras",
            "Ana Gómez                     | ana3@veterinaria.tech     | abcdefgh    | contraseña sin números",
    })
    void rechazaRegistroConDatosMalFormados(String nombre, String email, String password, String caso) {
        RegisterRequest registro = new RegisterRequest(nombre, email, password, RolUsuario.VETERINARIO);

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class);
    }

    @Test
    void aceptaNombresConTildesApostrofosYGuiones() {
        RegisterRequest registro = new RegisterRequest("María José O'Neil-Peña", "maria.jose@veterinaria.tech",
                "password123", RolUsuario.VETERINARIO);

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void rechazaLoginConContrasenaDemasiadoLarga() {
        LoginRequest login = new LoginRequest("ada@veterinaria.tech", "a1".repeat(5000));

        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(login)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiError.class);
    }

    @Test
    void noPermiteCrearCuentasDePropietario() {
        RegisterRequest registro = new RegisterRequest("Laura Gómez", "laura.duena@veterinaria.tech", "password123",
                RolUsuario.PROPIETARIO);

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ApiError.class);
    }

    @Test
    void unaCuentaDePropietarioAntiguaNoEntraAlPanel() {
        usuarioService.crear("Dueño Antiguo", "antiguo@veterinaria.tech", passwordEncoder.encode("password123"),
                RolUsuario.PROPIETARIO);

        restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("antiguo@veterinaria.tech", "password123"))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(ApiError.class);
    }

    @Test
    void rechazaNombreYCorreoDeMasDeCienCaracteres() {
        String nombreLargo = "a".repeat(101);
        String correoLargo = "a".repeat(90) + "@clinica.test";

        for (RegisterRequest registro : new RegisterRequest[] {
                new RegisterRequest(nombreLargo, "largo1@veterinaria.tech", "password123", RolUsuario.VETERINARIO),
                new RegisterRequest("Ana Gómez", correoLargo, "password123", RolUsuario.VETERINARIO) }) {
            restTestClient.post().uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(registro)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody(ApiError.class);
        }

        restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("a".repeat(100), "cien@veterinaria.tech", "password123", RolUsuario.VETERINARIO))
                .exchange()
                .expectStatus().isCreated();
    }
}
