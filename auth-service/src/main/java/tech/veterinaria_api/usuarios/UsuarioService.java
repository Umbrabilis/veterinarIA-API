package tech.veterinaria_api.usuarios;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.ConflictoException;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.common.RolUsuario;
import tech.veterinaria_api.usuarios.dto.ActualizarUsuarioRequest;
import tech.veterinaria_api.usuarios.dto.CrearUsuarioRequest;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** Roles del personal: los únicos que usan el sistema y que gestiona el módulo de usuarios. */
    private static final List<RolUsuario> ROLES_PERSONAL = List.of(RolUsuario.ADMINISTRADOR, RolUsuario.VETERINARIO);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Usuario obtener(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    public Usuario crear(String nombre, String email, String passwordHash, RolUsuario rol) {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .email(email)
                .passwordHash(passwordHash)
                .rol(rol)
                .activo(true)
                .build();
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarNombre(UUID id, String nombre) {
        Usuario usuario = obtener(id);
        usuario.setNombre(nombre.trim());
        return usuario;
    }

    @Transactional
    public void cambiarPassword(UUID id, String passwordActual, String passwordNueva) {
        Usuario usuario = obtener(id);
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarVeterinarios() {
        return usuarioRepository.findByRolAndActivoTrueOrderByNombreAsc(RolUsuario.VETERINARIO);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerVeterinario(UUID id) {
        return usuarioRepository.findByIdAndRolAndActivoTrue(id, RolUsuario.VETERINARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado"));
    }

    // --- Gestión del personal (solo administradores) ---

    @Transactional(readOnly = true)
    public List<Usuario> listarPersonal() {
        return usuarioRepository.findByRolInOrderByNombreAsc(ROLES_PERSONAL);
    }

    @Transactional
    public Usuario crearPersonal(CrearUsuarioRequest request) {
        exigirRolDePersonal(request.rol());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictoException("Ya existe una cuenta con ese email");
        }
        return crear(request.nombre().trim(), email, passwordEncoder.encode(request.password()), request.rol());
    }

    /**
     * Un administrador no puede quitarse el rol ni desactivarse a sí mismo: así el sistema nunca se queda sin
     * nadie que pueda gestionar las cuentas.
     */
    @Transactional
    public Usuario actualizarPersonal(UUID id, ActualizarUsuarioRequest request, UUID administradorId) {
        exigirRolDePersonal(request.rol());
        Usuario usuario = obtenerPersonal(id);
        if (usuario.getId().equals(administradorId)
                && (request.rol() != RolUsuario.ADMINISTRADOR || !request.activo())) {
            throw new ReglaNegocioException("No puedes quitarte el rol de administrador ni desactivar tu propia cuenta");
        }
        usuario.setNombre(request.nombre().trim());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        return usuario;
    }

    private Usuario obtenerPersonal(UUID id) {
        return usuarioRepository.findById(id)
                .filter(u -> ROLES_PERSONAL.contains(u.getRol()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private static void exigirRolDePersonal(RolUsuario rol) {
        if (!ROLES_PERSONAL.contains(rol)) {
            throw new ReglaNegocioException("El sistema solo admite cuentas de administrador o veterinario");
        }
    }
}
