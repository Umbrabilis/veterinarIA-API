package tech.veterinaria_api.usuarios;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tech.veterinaria_api.common.RecursoNoEncontradoException;
import tech.veterinaria_api.common.ReglaNegocioException;
import tech.veterinaria_api.common.RolUsuario;

@Service
@RequiredArgsConstructor
public class UsuarioService {

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
}
