package tech.veterinaria_api.consultas;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consulta {

    @Id
    private UUID id;

    @Column(name = "mascota_id", nullable = false)
    private UUID mascotaId;

    @Column(name = "propietario_id", nullable = false)
    private UUID propietarioId;

    @Column(name = "veterinario_id", nullable = false)
    private UUID veterinarioId;

    @Column(name = "cita_id")
    private UUID citaId;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(columnDefinition = "text")
    private String sintomas;

    @Column(name = "examen_fisico", columnDefinition = "text")
    private String examenFisico;

    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "temperatura_c", precision = 4, scale = 1)
    private BigDecimal temperaturaC;

    @Column(columnDefinition = "text")
    private String diagnostico;

    @Column(columnDefinition = "text")
    private String tratamiento;

    @Column(columnDefinition = "text")
    private String indicaciones;

    @Column(name = "proximo_control")
    private LocalDate proximoControl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoConsulta estado;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean estaCerrada() {
        return estado == EstadoConsulta.CERRADA;
    }
}
