package tech.veterinaria_api.duenos;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "duenos")
@Getter @Setter @NoArgsConstructor
public class Dueno {
    @Id private UUID id;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(nullable = false, length = 255) private String email;
    @Column(nullable = false, length = 30) private String telefono;
    @Column(length = 255) private String direccion;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
