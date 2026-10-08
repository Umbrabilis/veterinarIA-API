package tech.veterinaria_api.consultas;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {

    List<Consulta> findByMascotaIdOrderByCreatedAtDesc(UUID mascotaId);

    List<Consulta> findByMascotaIdAndEstadoOrderByCreatedAtDesc(UUID mascotaId, EstadoConsulta estado);

    /** Controles pendientes: consultas cerradas con control en el rango y sin una consulta posterior. */
    @Query("""
            select c from Consulta c
            where c.estado = tech.veterinaria_api.consultas.EstadoConsulta.CERRADA
              and c.proximoControl between :desde and :hasta
              and not exists (
                  select 1 from Consulta posterior
                  where posterior.mascotaId = c.mascotaId and posterior.createdAt > c.cerradaEn)
            order by c.proximoControl
            """)
    List<Consulta> controlesPendientes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
