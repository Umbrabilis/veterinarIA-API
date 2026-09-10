package tech.veterinaria_api.mascotas;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "mascotas")
@Getter @Setter @NoArgsConstructor
public class Mascota {
    @Id private UUID id;
    @Column(name = "dueno_id", nullable = false) private UUID duenoId;
    @Column(nullable = false, length = 150) private String nombre;
    @Column(nullable = false, length = 80) private String especie;
    @Column(length = 100) private String raza;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SexoMascota sexo;
    @Column(name = "fecha_nacimiento") private LocalDate fechaNacimiento;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
