package tech.veterinaria_api.mascotas;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaRepository extends JpaRepository<Mascota, UUID> {

    Page<Mascota> findByPropietarioId(UUID propietarioId, Pageable pageable);

    List<Mascota> findByPropietarioIdOrderByNombreAsc(UUID propietarioId);
}
