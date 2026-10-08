package tech.veterinaria_api.consultas;

import java.time.Instant;
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

/** Corrección de una consulta cerrada, con autor y fecha. Solo inserción (lo refuerza un trigger). */
@Entity
@Immutable
@Table(name = "enmiendas")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enmienda {

    @Id
    private UUID id;

    @Column(name = "consulta_id", nullable = false)
    private UUID consultaId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
