package tech.veterinaria_api.usuarios;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.auth.dto.UsuarioResponse;
import tech.veterinaria_api.security.UsuarioActual;
import tech.veterinaria_api.usuarios.dto.ActualizarPerfilRequest;
import tech.veterinaria_api.usuarios.dto.ActualizarUsuarioRequest;
import tech.veterinaria_api.usuarios.dto.CambiarPasswordRequest;
import tech.veterinaria_api.usuarios.dto.CrearUsuarioRequest;
import tech.veterinaria_api.usuarios.dto.UsuarioAdminResponse;
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

    // --- Gestión del personal: solo administradores ---

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Lista las cuentas del personal (administradores y veterinarios)")
    public List<UsuarioAdminResponse> listar() {
        return usuarioService.listarPersonal().stream().map(UsuarioAdminResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crea la cuenta de un administrador o veterinario con una contraseña inicial")
    public UsuarioAdminResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return UsuarioAdminResponse.de(usuarioService.crearPersonal(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cambia nombre, rol o estado de una cuenta del personal")
    public UsuarioAdminResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarUsuarioRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return UsuarioAdminResponse.de(usuarioService.actualizarPersonal(id, request, UsuarioActual.de(jwt).id()));
    }
}
