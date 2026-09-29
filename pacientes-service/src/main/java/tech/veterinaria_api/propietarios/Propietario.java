package tech.veterinaria_api.propietarios;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "propietarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Propietario {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String documento;

    @Column(nullable = false, length = 30)
    private String telefono;

    private String email;

    private String direccion;

    @Column(name = "consentimiento_datos_en", nullable = false)
    private Instant consentimientoDatosEn;

    @Column(name = "codigo_vinculacion_hash", length = 64)
    private String codigoVinculacionHash;

    @Column(name = "codigo_vinculacion_expira_en")
    private Instant codigoVinculacionExpiraEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
