package tech.veterinaria_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import tech.veterinaria_api.TestcontainersConfiguration;
import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.ApiError;
import tech.veterinaria_api.usuarios.RolUsuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AuthControllerTest {

    @Autowired
    private RestTestClient restTestClient;

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
}
