package tech.veterinaria_api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tech.veterinaria_api.TestcontainersConfiguration;
import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.common.ApiError;
import tech.veterinaria_api.usuarios.RolUsuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registraLogueaYConsultaElUsuarioAutenticado() {
        RegisterRequest registro = new RegisterRequest("Ada Lovelace", "ada@veterinaria.tech", "password123",
                RolUsuario.VETERINARIO);

        ResponseEntity<AuthResponse> registroResponse = restTemplate.postForEntity("/api/v1/auth/register",
                registro, AuthResponse.class);

        assertThat(registroResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registroResponse.getBody()).isNotNull();
        assertThat(registroResponse.getBody().accessToken()).isNotBlank();
        assertThat(registroResponse.getBody().usuario().email()).isEqualTo("ada@veterinaria.tech");
        assertThat(registroResponse.getBody().usuario().rol()).isEqualTo(RolUsuario.VETERINARIO);

        LoginRequest login = new LoginRequest("ada@veterinaria.tech", "password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/api/v1/auth/login", login,
                AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResponse.getBody().accessToken();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<UsuarioResponse> meResponse = restTemplate.exchange("/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), UsuarioResponse.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().email()).isEqualTo("ada@veterinaria.tech");
    }

    @Test
    void rechazaRegistroConEmailDuplicado() {
        RegisterRequest registro = new RegisterRequest("Grace Hopper", "grace@veterinaria.tech", "password123",
                RolUsuario.ADMINISTRADOR);
        restTemplate.postForEntity("/api/v1/auth/register", registro, AuthResponse.class);

        ResponseEntity<ApiError> segundoIntento = restTemplate.postForEntity("/api/v1/auth/register", registro,
                ApiError.class);

        assertThat(segundoIntento.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void rechazaLoginConCredencialesInvalidas() {
        LoginRequest login = new LoginRequest("no-existe@veterinaria.tech", "loquesea123");

        ResponseEntity<ApiError> response = restTemplate.postForEntity("/api/v1/auth/login", login, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
