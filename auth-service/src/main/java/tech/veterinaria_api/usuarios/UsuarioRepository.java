package tech.veterinaria_api.usuarios;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.veterinaria_api.common.RolUsuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByRolAndActivoTrueOrderByNombreAsc(RolUsuario rol);

    Optional<Usuario> findByIdAndRolAndActivoTrue(UUID id, RolUsuario rol);

    List<Usuario> findByRolInOrderByNombreAsc(Collection<RolUsuario> roles);
}
