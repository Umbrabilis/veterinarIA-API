package tech.veterinaria_api.vacunas;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Vacuna aplicada (plan preventivo). Registro clínico de solo inserción. */
@Entity
@Immutable
@Table(name = "vacunas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vacuna {

    @Id
    private UUID id;

    @Column(name = "mascota_id", nullable = false)
    private UUID mascotaId;

    @Column(name = "propietario_id", nullable = false)
    private UUID propietarioId;

    @Column(name = "veterinario_id", nullable = false)
    private UUID veterinarioId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 60)
    private String lote;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "proxima_dosis")
    private LocalDate proximaDosis;

    @Column(columnDefinition = "text")
    private String notas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
