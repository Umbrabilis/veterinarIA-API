package tech.veterinaria_api.resumenes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ResumenConsultaRepository extends JpaRepository<ResumenConsulta, UUID> {

    Optional<ResumenConsulta> findByConsultaId(UUID consultaId);

    /** SELECT ... FOR UPDATE: serializa los envíos de un mismo resumen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResumenConsulta r where r.id = :id")
    Optional<ResumenConsulta> bloquear(@Param("id") UUID id);

    boolean existsByConsultaId(UUID consultaId);

    List<ResumenConsulta> findByVeterinarioIdAndEstadoOrderByCreatedAtDesc(UUID veterinarioId, EstadoResumen estado);

    List<ResumenConsulta> findByEstadoOrderByCreatedAtDesc(EstadoResumen estado);
}
