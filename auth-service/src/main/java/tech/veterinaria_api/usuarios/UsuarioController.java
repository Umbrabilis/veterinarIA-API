package tech.veterinaria_api.usuarios;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.security.UsuarioActual;
import tech.veterinaria_api.usuarios.dto.ActualizarPerfilRequest;
import tech.veterinaria_api.usuarios.dto.CambiarPasswordRequest;
import tech.veterinaria_api.usuarios.dto.VeterinarioResponse;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado y directorio de veterinarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado")
    public UsuarioResponse miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(usuarioService.obtener(UsuarioActual.de(jwt).id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Actualiza el nombre del usuario autenticado")
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return UsuarioResponse.de(usuarioService.actualizarNombre(UsuarioActual.de(jwt).id(), request.nombre()));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambia la contraseña del usuario autenticado")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPassword(UsuarioActual.de(jwt).id(), request.passwordActual(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/veterinarios")
    @Operation(summary = "Veterinarios activos (para agendar citas)")
    public List<VeterinarioResponse> veterinarios() {
        return usuarioService.listarVeterinarios().stream().map(VeterinarioResponse::de).toList();
    }

    @GetMapping("/veterinarios/{id}")
    @Operation(summary = "Un veterinario activo por id (lo usa citas-service para validar la cita)")
    public VeterinarioResponse veterinario(@PathVariable UUID id) {
        return VeterinarioResponse.de(usuarioService.obtenerVeterinario(id));
    }
}
