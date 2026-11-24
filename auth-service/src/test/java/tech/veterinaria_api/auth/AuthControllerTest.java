package tech.veterinaria_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.testing.TestcontainersConfiguration;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.ApiError;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.auth.SesionDePrueba.Cuenta;
import tech.veterinaria_api.usuarios.UsuarioService;
import tech.veterinaria_api.usuarios.dto.ActualizarPerfilRequest;

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
    void registraIniciaSesionConCookieYConsultaElUsuarioAutenticado() {
        RegisterRequest registro = new RegisterRequest("Ada Lovelace", "ada@veterinaria.tech", "password123",
                RolUsuario.VETERINARIO);

        // El registro crea la cuenta pero no inicia sesión (lo usa también un admin para crear cuentas ajenas).
        EntityExchangeResult<UsuarioResponse> registroResultado = restTestClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UsuarioResponse.class)
                .returnResult();
        assertThat(registroResultado.getResponseHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(registroResultado.getResponseBody().email()).isEqualTo("ada@veterinaria.tech");
        assertThat(registroResultado.getResponseBody().rol()).isEqualTo(RolUsuario.VETERINARIO);

        EntityExchangeResult<String> login = restTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("ada@veterinaria.tech", "password123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        // El JWT viaja solo en la cookie HttpOnly: el cuerpo no lo trae.
        String setCookie = login.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith(SesionDePrueba.COOKIE + "=")
                .contains("HttpOnly", "Path=/api", "SameSite=Strict", "Max-Age=3600");
        assertThat(login.getResponseBody()).doesNotContain("accessToken", "eyJ")
                .contains("\"expiresInSeconds\":3600", "ada@veterinaria.tech");

        UsuarioResponse me = restTestClient.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.COOKIE, SesionDePrueba.COOKIE + "=" + SesionDePrueba.tokenDeCookie(login))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UsuarioResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(me).isNotNull();
        assertThat(me.email()).isEqualTo("ada@veterinaria.tech");
    }

    @Test
    void cerrarSesionBorraLaCookie() {
        Cuenta cuenta = SesionDePrueba.registrarEIniciarSesion(restTestClient, "Cerrar Sesion", "logout@veterinaria.tech",
                RolUsuario.VETERINARIO);

        String setCookie = restTestClient.post().uri("/api/v1/auth/logout")
                .header(HttpHeaders.COOKIE, cuenta.cookie())
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange()
                .expectStatus().isNoContent()
                .returnResult()
                .getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);

        assertThat(setCookie).startsWith(SesionDePrueba.COOKIE + "=;").contains("Max-Age=0", "HttpOnly");
    }

    @Test
    void sinSesionNiTokenRespondeNoAutenticado() {
        restTestClient.get().uri("/api/v1/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
        restTestClient.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.COOKIE, SesionDePrueba.COOKIE + "=token-alterado")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void unaCookieVencidaOInvalidaNoImpideIniciarSesion() {
        SesionDePrueba.registrarEIniciarSesion(restTestClient, "Cookie Vieja", "vieja@veterinaria.tech",
                RolUsuario.VETERINARIO);

        restTestClient.post().uri("/api/v1/auth/login")
                .header(HttpHeaders.COOKIE, SesionDePrueba.COOKIE + "=token-vencido")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("vieja@veterinaria.tech", SesionDePrueba.PASSWORD))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void conCookieSoloSeAceptanCambiosDesdeUnOrigenPermitido() {
        Cuenta cuenta = SesionDePrueba.registrarEIniciarSesion(restTestClient, "Origen Prueba",
                "origen@veterinaria.tech", RolUsuario.VETERINARIO);
        ActualizarPerfilRequest cambio = new ActualizarPerfilRequest("Origen Cambiado");

        // CSRF: otra página no puede usar la cookie del usuario para cambiar datos. CORS rechaza el origen ajeno...
        restTestClient.put().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.COOKIE, cuenta.cookie())
                .header(HttpHeaders.ORIGIN, "https://sitio-malicioso.test")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambio)
                .exchange()
                .expectStatus().isForbidden();
        // ...y OrigenPermitidoFilter rechaza la petición con cookie que no dice de dónde viene.
        restTestClient.put().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.COOKIE, cuenta.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambio)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody(ApiError.class);

        // Desde el frontend permitido sí.
        restTestClient.put().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.COOKIE, cuenta.cookie())
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cambio)
                .exchange()
                .expectStatus().isOk();

        // Las lecturas no cambian datos: no exigen origen.
        restTestClient.get().uri("/api/v1/usuarios/me")
                .header(HttpHeaders.COOKIE, cuenta.cookie())
                .exchange()
                .expectStatus().isOk();
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
