package tech.veterinaria_api.usuarios;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
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
}
