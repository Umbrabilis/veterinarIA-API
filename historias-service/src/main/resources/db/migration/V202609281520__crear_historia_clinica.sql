-- Historia clínica. Regla de negocio: es inmutable. Una consulta cerrada no se edita ni se borra; se corrige
-- con una enmienda que registra quién y cuándo. Las enmiendas y las vacunas aplicadas solo se insertan.
-- Los triggers hacen cumplir la regla aunque alguien escriba en la base de datos sin pasar por la API.

CREATE TABLE consultas (
    id              UUID PRIMARY KEY,
    mascota_id      UUID         NOT NULL,
    propietario_id  UUID         NOT NULL,
    veterinario_id  UUID         NOT NULL,
    cita_id         UUID,
    motivo          VARCHAR(500) NOT NULL,
    sintomas        TEXT,
    examen_fisico   TEXT,
    peso_kg         NUMERIC(6, 2) CHECK (peso_kg > 0),
    temperatura_c   NUMERIC(4, 1) CHECK (temperatura_c BETWEEN 25 AND 45),
    diagnostico     TEXT,
    tratamiento     TEXT,
    indicaciones    TEXT,
    proximo_control DATE,
    estado          VARCHAR(20)  NOT NULL CHECK (estado IN ('ABIERTA', 'CERRADA')),
    cerrada_en      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_consultas_cierre CHECK ((estado = 'CERRADA') = (cerrada_en IS NOT NULL)),
    CONSTRAINT ck_consultas_cerrada_con_diagnostico CHECK (estado = 'ABIERTA' OR diagnostico IS NOT NULL)
);

COMMENT ON COLUMN consultas.propietario_id IS 'Copia del propietario de la mascota al crear la consulta (autorización por pertenencia)';

CREATE INDEX idx_consultas_mascota ON consultas (mascota_id, created_at DESC);
CREATE INDEX idx_consultas_veterinario ON consultas (veterinario_id, created_at DESC);
CREATE INDEX idx_consultas_proximo_control ON consultas (proximo_control) WHERE estado = 'CERRADA';

CREATE TABLE enmiendas (
    id          UUID PRIMARY KEY,
    consulta_id UUID         NOT NULL REFERENCES consultas (id),
    autor_id    UUID         NOT NULL,
    motivo      VARCHAR(500) NOT NULL,
    contenido   TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_enmiendas_consulta ON enmiendas (consulta_id, created_at);

CREATE TABLE vacunas (
    id               UUID PRIMARY KEY,
    mascota_id       UUID         NOT NULL,
    propietario_id   UUID         NOT NULL,
    veterinario_id   UUID         NOT NULL,
    nombre           VARCHAR(150) NOT NULL,
    lote             VARCHAR(60),
    fecha_aplicacion DATE         NOT NULL,
    proxima_dosis    DATE,
    notas            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_vacunas_proxima_dosis CHECK (proxima_dosis IS NULL OR proxima_dosis > fecha_aplicacion)
);

CREATE INDEX idx_vacunas_mascota ON vacunas (mascota_id, fecha_aplicacion DESC);
CREATE INDEX idx_vacunas_proxima_dosis ON vacunas (proxima_dosis) WHERE proxima_dosis IS NOT NULL;

-- Una consulta abierta se puede editar; una cerrada no. Ninguna se borra.
CREATE FUNCTION impedir_cambios_consulta_cerrada() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'La historia clínica no se elimina (consulta %)', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    IF OLD.estado = 'CERRADA' THEN
        RAISE EXCEPTION 'La consulta % está cerrada: no se edita, se enmienda', OLD.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_consultas_inmutables
    BEFORE UPDATE OR DELETE ON consultas
    FOR EACH ROW EXECUTE FUNCTION impedir_cambios_consulta_cerrada();

-- Enmiendas y vacunas: registros de solo inserción.
CREATE FUNCTION impedir_modificacion_registro_clinico() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El registro clínico % de % no se modifica ni se elimina', OLD.id, TG_TABLE_NAME
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enmiendas_solo_insercion
    BEFORE UPDATE OR DELETE ON enmiendas
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_registro_clinico();

CREATE TRIGGER trg_vacunas_solo_insercion
    BEFORE UPDATE OR DELETE ON vacunas
    FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_registro_clinico();

-- Solo se enmiendan consultas cerradas (las abiertas se editan directamente).
CREATE FUNCTION exigir_consulta_cerrada_para_enmienda() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM consultas WHERE id = NEW.consulta_id AND estado = 'CERRADA') THEN
        RAISE EXCEPTION 'Solo se enmiendan consultas cerradas (consulta %)', NEW.consulta_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enmiendas_consulta_cerrada
    BEFORE INSERT ON enmiendas
    FOR EACH ROW EXECUTE FUNCTION exigir_consulta_cerrada_para_enmienda();
