package tech.veterinaria_api.consultas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnmiendaRepository extends JpaRepository<Enmienda, UUID> {

    List<Enmienda> findByConsultaIdOrderByCreatedAtAsc(UUID consultaId);
}
