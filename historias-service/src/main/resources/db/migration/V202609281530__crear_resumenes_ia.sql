-- Resúmenes de consulta para el dueño, redactados por IA y aprobados por el veterinario.
-- Regla crítica: ningún resumen se envía sin aprobación del veterinario. Se hace cumplir aquí, no solo en la API:
--   * los CHECK exigen aprobador, fecha y contenido final para APROBADO/ENVIADO;
--   * el trigger solo permite BORRADOR -> APROBADO -> ENVIADO (sin saltos ni retrocesos), nace en BORRADOR,
--     y congela el contenido aprobado y lo generado por el modelo (trazabilidad: modelo, versión de prompt,
--     contenido generado y contenido final aprobado).

CREATE TABLE resumenes_consulta (
    id                      UUID PRIMARY KEY,
    consulta_id             UUID         NOT NULL UNIQUE REFERENCES consultas (id),
    veterinario_id          UUID         NOT NULL,
    modelo                  VARCHAR(100) NOT NULL,
    version_prompt          VARCHAR(50)  NOT NULL,
    generado_hallazgos      TEXT         NOT NULL,
    generado_tratamiento    TEXT         NOT NULL,
    generado_cuidados       TEXT         NOT NULL,
    generado_proxima_visita TEXT         NOT NULL,
    final_hallazgos         TEXT,
    final_tratamiento       TEXT,
    final_cuidados          TEXT,
    final_proxima_visita    TEXT,
    estado                  VARCHAR(20)  NOT NULL CHECK (estado IN ('BORRADOR', 'APROBADO', 'ENVIADO')),
    aprobado_por            UUID,
    aprobado_en             TIMESTAMPTZ,
    enviado_en              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_resumenes_aprobacion_completa CHECK (
        estado = 'BORRADOR' OR (
            aprobado_por IS NOT NULL AND aprobado_en IS NOT NULL
            AND final_hallazgos IS NOT NULL AND final_tratamiento IS NOT NULL
            AND final_cuidados IS NOT NULL AND final_proxima_visita IS NOT NULL)),
    CONSTRAINT ck_resumenes_borrador_sin_aprobacion CHECK (
        estado <> 'BORRADOR' OR (aprobado_por IS NULL AND aprobado_en IS NULL)),
    CONSTRAINT ck_resumenes_envio CHECK ((estado = 'ENVIADO') = (enviado_en IS NOT NULL)),
    -- Aprueba el veterinario que atendió la consulta.
    CONSTRAINT ck_resumenes_aprueba_su_veterinario CHECK (aprobado_por IS NULL OR aprobado_por = veterinario_id)
);

COMMENT ON COLUMN resumenes_consulta.generado_hallazgos IS 'Texto tal como lo generó el modelo (no se modifica)';
COMMENT ON COLUMN resumenes_consulta.final_hallazgos IS 'Texto revisado y aprobado por el veterinario';

CREATE INDEX idx_resumenes_veterinario_estado ON resumenes_consulta (veterinario_id, estado);

CREATE FUNCTION proteger_resumen_consulta() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.estado <> 'BORRADOR' THEN
            RAISE EXCEPTION 'Un resumen nace como BORRADOR; requiere aprobación del veterinario'
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM consultas WHERE id = NEW.consulta_id AND estado = 'CERRADA') THEN
            RAISE EXCEPTION 'El resumen se genera sobre una consulta cerrada (consulta %)', NEW.consulta_id
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los resúmenes no se eliminan (resumen %)', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.created_at IS DISTINCT FROM OLD.created_at
        OR NEW.consulta_id IS DISTINCT FROM OLD.consulta_id
        OR NEW.veterinario_id IS DISTINCT FROM OLD.veterinario_id
        OR NEW.modelo IS DISTINCT FROM OLD.modelo
        OR NEW.version_prompt IS DISTINCT FROM OLD.version_prompt
        OR NEW.generado_hallazgos IS DISTINCT FROM OLD.generado_hallazgos
        OR NEW.generado_tratamiento IS DISTINCT FROM OLD.generado_tratamiento
        OR NEW.generado_cuidados IS DISTINCT FROM OLD.generado_cuidados
        OR NEW.generado_proxima_visita IS DISTINCT FROM OLD.generado_proxima_visita THEN
        RAISE EXCEPTION 'El contenido generado por el modelo no se modifica (resumen %)', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF OLD.estado = 'ENVIADO' THEN
        RAISE EXCEPTION 'Un resumen enviado no se modifica (resumen %)', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF OLD.estado = 'BORRADOR' AND NEW.estado NOT IN ('BORRADOR', 'APROBADO') THEN
        RAISE EXCEPTION 'Un resumen no se envía sin aprobación del veterinario (resumen %)', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF OLD.estado = 'APROBADO' THEN
        IF NEW.estado NOT IN ('APROBADO', 'ENVIADO')
            OR NEW.final_hallazgos IS DISTINCT FROM OLD.final_hallazgos
            OR NEW.final_tratamiento IS DISTINCT FROM OLD.final_tratamiento
            OR NEW.final_cuidados IS DISTINCT FROM OLD.final_cuidados
            OR NEW.final_proxima_visita IS DISTINCT FROM OLD.final_proxima_visita
            OR NEW.aprobado_por IS DISTINCT FROM OLD.aprobado_por
            OR NEW.aprobado_en IS DISTINCT FROM OLD.aprobado_en THEN
            RAISE EXCEPTION 'El contenido aprobado no se modifica (resumen %)', OLD.id
                USING ERRCODE = 'integrity_constraint_violation';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public, pg_temp;

CREATE TRIGGER trg_resumenes_aprobacion_obligatoria
    BEFORE INSERT OR UPDATE OR DELETE ON resumenes_consulta
    FOR EACH ROW EXECUTE FUNCTION proteger_resumen_consulta();

CREATE TRIGGER trg_resumenes_no_truncate
    BEFORE TRUNCATE ON resumenes_consulta
    FOR EACH STATEMENT EXECUTE FUNCTION impedir_truncate_registro_clinico();
