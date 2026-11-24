package tech.veterinaria_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.RolUsuario;

/** Registra una cuenta e inicia sesión, como lo hace el frontend; el JWT se toma de la cookie HttpOnly. */
public final class SesionDePrueba {

    public static final String COOKIE = "veterinaria_sesion";
    public static final String PASSWORD = "password123";

    /** Cuenta con sesión iniciada: {@code token} es el valor de la cookie de sesión. */
    public record Cuenta(UsuarioResponse usuario, String token) {

        public String bearer() {
            return "Bearer " + token;
        }

        public String cookie() {
            return COOKIE + "=" + token;
        }
    }

    private SesionDePrueba() {
    }

    public static Cuenta registrarEIniciarSesion(RestTestClient cliente, String nombre, String email, RolUsuario rol) {
        cliente.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(nombre, email, PASSWORD, rol))
                .exchange()
                .expectStatus().isCreated();
        return iniciarSesion(cliente, email, PASSWORD);
    }

    public static Cuenta iniciarSesion(RestTestClient cliente, String email, String password) {
        EntityExchangeResult<AuthResponse> resultado = cliente.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult();
        assertThat(resultado.getResponseBody()).isNotNull();
        return new Cuenta(resultado.getResponseBody().usuario(), tokenDeCookie(resultado));
    }

    public static String tokenDeCookie(EntityExchangeResult<?> resultado) {
        String setCookie = resultado.getResponseHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie de la sesión").startsWith(COOKIE + "=");
        return setCookie.substring(COOKIE.length() + 1, setCookie.indexOf(';'));
    }
}
