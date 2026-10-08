package tech.veterinaria_api.mascotas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MascotaRepository extends JpaRepository<Mascota, UUID> {

    Page<Mascota> findByPropietarioId(UUID propietarioId, Pageable pageable);

    List<Mascota> findByPropietarioIdOrderByNombreAsc(UUID propietarioId);

    /** Por nombre de la mascota, nombre del propietario o documento exacto del propietario. */
    @Query("""
            select m from Mascota m
            where lower(m.nombre) like lower(concat('%', :busqueda, '%'))
               or m.propietarioId in (
                   select p.id from Propietario p
                   where lower(p.nombre) like lower(concat('%', :busqueda, '%'))
                      or p.documento = :busqueda)
            """)
    Page<Mascota> buscar(@Param("busqueda") String busqueda, Pageable pageable);

    @Query("""
            select m from Mascota m
            where m.propietarioId = :propietarioId
              and lower(m.nombre) like lower(concat('%', :busqueda, '%'))
            """)
    Page<Mascota> buscarDePropietario(@Param("propietarioId") UUID propietarioId, @Param("busqueda") String busqueda,
            Pageable pageable);
}
