package tech.veterinaria_api.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.auth.dto.AuthResponse;
import tech.veterinaria_api.auth.dto.LoginRequest;
import tech.veterinaria_api.auth.dto.RegisterRequest;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.security.UsuarioActual;
import tech.veterinaria_api.usuarios.UsuarioService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, login y sesión del usuario autenticado")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;
    private final CookieSesion cookieSesion;

    @PostMapping("/register")
    @Operation(summary = "Crea una cuenta. No inicia sesión: después se usa /login")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión: el JWT queda en una cookie HttpOnly; el cuerpo trae el usuario")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.SesionEmitida sesion = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieSesion.con(sesion.token()).toString())
                .body(sesion.respuesta());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra la sesión borrando la cookie")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieSesion.vencida().toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(UsuarioResponse.de(usuarioService.obtener(UsuarioActual.de(jwt).id())));
    }
}
