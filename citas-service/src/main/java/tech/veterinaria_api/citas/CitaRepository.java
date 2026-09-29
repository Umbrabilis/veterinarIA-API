package tech.veterinaria_api.citas;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CitaRepository extends JpaRepository<Cita, UUID>, JpaSpecificationExecutor<Cita> {

    List<Cita> findByPropietarioIdOrderByInicioDesc(UUID propietarioId);

    List<Cita> findByVeterinarioIdAndInicioGreaterThanEqualOrderByInicioAsc(UUID veterinarioId, Instant desde);

    static Specification<Cita> filtros(UUID veterinarioId, UUID mascotaId, Instant desde, Instant hasta) {
        return (root, query, cb) -> {
            var predicados = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (veterinarioId != null) {
                predicados.add(cb.equal(root.get("veterinarioId"), veterinarioId));
            }
            if (mascotaId != null) {
                predicados.add(cb.equal(root.get("mascotaId"), mascotaId));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("inicio"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThan(root.get("inicio"), hasta));
            }
            return cb.and(predicados.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
