package tech.veterinaria_api.resumenes;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Resumen de una consulta para el dueño. El contenido generado por el modelo es de solo lectura; el veterinario
 * edita el contenido final y lo aprueba. Las transiciones de estado solo ocurren mediante los métodos de esta
 * clase y la base de datos las valida de nuevo.
 */
@Entity
@Table(name = "resumenes_consulta")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ResumenConsulta {

    @Id
    private UUID id;

    @Column(name = "consulta_id", nullable = false, updatable = false)
    private UUID consultaId;

    @Column(name = "veterinario_id", nullable = false, updatable = false)
    private UUID veterinarioId;

    @Column(nullable = false, length = 100, updatable = false)
    private String modelo;

    @Column(name = "version_prompt", nullable = false, length = 50, updatable = false)
    private String versionPrompt;

    @Column(name = "generado_hallazgos", nullable = false, updatable = false, columnDefinition = "text")
    private String generadoHallazgos;

    @Column(name = "generado_tratamiento", nullable = false, updatable = false, columnDefinition = "text")
    private String generadoTratamiento;

    @Column(name = "generado_cuidados", nullable = false, updatable = false, columnDefinition = "text")
    private String generadoCuidados;

    @Column(name = "generado_proxima_visita", nullable = false, updatable = false, columnDefinition = "text")
    private String generadoProximaVisita;

    @Column(name = "final_hallazgos", columnDefinition = "text")
    private String finalHallazgos;

    @Column(name = "final_tratamiento", columnDefinition = "text")
    private String finalTratamiento;

    @Column(name = "final_cuidados", columnDefinition = "text")
    private String finalCuidados;

    @Column(name = "final_proxima_visita", columnDefinition = "text")
    private String finalProximaVisita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResumen estado;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(name = "aprobado_en")
    private Instant aprobadoEn;

    @Column(name = "enviado_en")
    private Instant enviadoEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ResumenConsulta borrador(UUID consultaId, UUID veterinarioId, String modelo, String versionPrompt,
            ContenidoResumen generado) {
        return ResumenConsulta.builder()
                .id(UUID.randomUUID())
                .consultaId(consultaId)
                .veterinarioId(veterinarioId)
                .modelo(modelo)
                .versionPrompt(versionPrompt)
                .generadoHallazgos(generado.hallazgos())
                .generadoTratamiento(generado.tratamiento())
                .generadoCuidados(generado.cuidadosEnCasa())
                .generadoProximaVisita(generado.proximaVisita())
                .estado(EstadoResumen.BORRADOR)
                .build();
    }

    public ContenidoResumen generado() {
        return new ContenidoResumen(generadoHallazgos, generadoTratamiento, generadoCuidados, generadoProximaVisita);
    }

    /** Contenido final editado por el veterinario, o null si aún no lo ha editado. */
    public ContenidoResumen contenidoFinal() {
        if (finalHallazgos == null) {
            return null;
        }
        return new ContenidoResumen(finalHallazgos, finalTratamiento, finalCuidados, finalProximaVisita);
    }

    void editar(ContenidoResumen contenido) {
        if (estado != EstadoResumen.BORRADOR) {
            throw new IllegalStateException("Solo se edita un borrador");
        }
        finalHallazgos = contenido.hallazgos().trim();
        finalTratamiento = contenido.tratamiento().trim();
        finalCuidados = contenido.cuidadosEnCasa().trim();
        finalProximaVisita = contenido.proximaVisita().trim();
    }

    void aprobar(UUID veterinario, Instant cuando) {
        if (estado != EstadoResumen.BORRADOR) {
            throw new IllegalStateException("Solo se aprueba un borrador");
        }
        if (contenidoFinal() == null) {
            editar(generado());
        }
        estado = EstadoResumen.APROBADO;
        aprobadoPor = veterinario;
        aprobadoEn = cuando;
    }

    void marcarEnviado(Instant cuando) {
        if (estado != EstadoResumen.APROBADO) {
            throw new IllegalStateException("Solo se envía un resumen aprobado");
        }
        estado = EstadoResumen.ENVIADO;
        enviadoEn = cuando;
    }
}
