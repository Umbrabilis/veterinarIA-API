package tech.veterinaria_api.resumenes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenConsultaRepository extends JpaRepository<ResumenConsulta, UUID> {

    Optional<ResumenConsulta> findByConsultaId(UUID consultaId);

    boolean existsByConsultaId(UUID consultaId);

    List<ResumenConsulta> findByVeterinarioIdAndEstadoOrderByCreatedAtDesc(UUID veterinarioId, EstadoResumen estado);

    List<ResumenConsulta> findByEstadoOrderByCreatedAtDesc(EstadoResumen estado);
}
