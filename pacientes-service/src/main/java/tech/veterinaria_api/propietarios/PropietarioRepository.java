package tech.veterinaria_api.propietarios;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropietarioRepository extends JpaRepository<Propietario, UUID> {

    Optional<Propietario> findByUsuarioId(UUID usuarioId);

    Optional<Propietario> findByEmailIgnoreCaseAndUsuarioIdIsNull(String email);

    boolean existsByDocumento(String documento);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentoAndIdNot(String documento, UUID id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("""
            select p from Propietario p
            where lower(p.nombre) like lower(concat('%', :busqueda, '%'))
               or p.documento = :busqueda
            """)
    Page<Propietario> buscar(@Param("busqueda") String busqueda, Pageable pageable);
}
