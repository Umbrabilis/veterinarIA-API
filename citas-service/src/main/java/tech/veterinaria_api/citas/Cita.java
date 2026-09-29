package tech.veterinaria_api.citas;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "citas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cita {

    @Id
    private UUID id;

    @Column(name = "mascota_id", nullable = false)
    private UUID mascotaId;

    @Column(name = "propietario_id", nullable = false)
    private UUID propietarioId;

    @Column(name = "veterinario_id", nullable = false)
    private UUID veterinarioId;

    @Column(name = "mascota_nombre", nullable = false, length = 100)
    private String mascotaNombre;

    @Column(name = "veterinario_nombre", nullable = false, length = 150)
    private String veterinarioNombre;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCita estado;

    @Column(name = "creada_por", nullable = false)
    private UUID creadaPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
