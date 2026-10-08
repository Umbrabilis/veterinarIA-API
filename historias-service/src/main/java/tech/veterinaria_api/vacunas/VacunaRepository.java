package tech.veterinaria_api.vacunas;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VacunaRepository extends JpaRepository<Vacuna, UUID> {

    List<Vacuna> findByMascotaIdOrderByFechaAplicacionDesc(UUID mascotaId);

    /** Refuerzos pendientes: próxima dosis en el rango y sin una aplicación posterior de la misma vacuna. */
    @Query("""
            select v from Vacuna v
            where v.proximaDosis between :desde and :hasta
              and not exists (
                  select 1 from Vacuna posterior
                  where posterior.mascotaId = v.mascotaId
                    and lower(posterior.nombre) = lower(v.nombre)
                    and posterior.fechaAplicacion > v.fechaAplicacion)
            order by v.proximaDosis
            """)
    List<Vacuna> refuerzosPendientes(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
