package tech.veterinaria_api.duenos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuenoRepository extends JpaRepository<Dueno, UUID> {
}
