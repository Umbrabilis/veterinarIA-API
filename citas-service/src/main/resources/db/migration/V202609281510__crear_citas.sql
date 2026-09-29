-- Agenda de citas. La base de datos impide que un veterinario o una mascota tengan dos citas activas que se
-- solapen (EXCLUDE USING gist sobre rangos de tiempo), incluso con varias réplicas del servicio escribiendo a
-- la vez: la regla no depende de una verificación previa en la aplicación.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE citas (
    id                 UUID PRIMARY KEY,
    mascota_id         UUID         NOT NULL,
    propietario_id     UUID         NOT NULL,
    veterinario_id     UUID         NOT NULL,
    mascota_nombre     VARCHAR(100) NOT NULL,
    veterinario_nombre VARCHAR(150) NOT NULL,
    inicio             TIMESTAMPTZ  NOT NULL,
    fin                TIMESTAMPTZ  NOT NULL,
    motivo             VARCHAR(500) NOT NULL,
    estado             VARCHAR(20)  NOT NULL
        CHECK (estado IN ('PROGRAMADA', 'CONFIRMADA', 'CANCELADA', 'ATENDIDA', 'NO_ASISTIO')),
    creada_por         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_citas_rango CHECK (fin > inicio),
    CONSTRAINT ex_citas_veterinario_sin_solapamiento EXCLUDE USING gist (
        veterinario_id WITH =,
        tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado IN ('PROGRAMADA', 'CONFIRMADA')),
    CONSTRAINT ex_citas_mascota_sin_solapamiento EXCLUDE USING gist (
        mascota_id WITH =,
        tstzrange(inicio, fin, '[)') WITH &&
    ) WHERE (estado IN ('PROGRAMADA', 'CONFIRMADA'))
);

COMMENT ON COLUMN citas.mascota_nombre IS 'Copia del nombre al agendar (pacientes-service es la fuente de verdad)';
COMMENT ON COLUMN citas.veterinario_nombre IS 'Copia del nombre al agendar (auth-service es la fuente de verdad)';

CREATE INDEX idx_citas_propietario ON citas (propietario_id, inicio);
CREATE INDEX idx_citas_mascota ON citas (mascota_id, inicio);
CREATE INDEX idx_citas_veterinario_inicio ON citas (veterinario_id, inicio);
