-- Propietarios de mascotas. Datos personales (Ley 1581 de 2012): se registra cuándo se otorgó el consentimiento.
CREATE TABLE propietarios (
    id                      UUID PRIMARY KEY,
    usuario_id              UUID UNIQUE,
    nombre                  VARCHAR(150) NOT NULL,
    documento               VARCHAR(30)  NOT NULL UNIQUE,
    telefono                VARCHAR(30)  NOT NULL,
    email                   VARCHAR(255),
    direccion               VARCHAR(255),
    consentimiento_datos_en TIMESTAMPTZ  NOT NULL,
    codigo_vinculacion_hash VARCHAR(64) UNIQUE,
    codigo_vinculacion_expira_en TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_propietarios_codigo_completo
        CHECK ((codigo_vinculacion_hash IS NULL) = (codigo_vinculacion_expira_en IS NULL))
);

COMMENT ON COLUMN propietarios.usuario_id IS 'Cuenta de auth-service (rol PROPIETARIO) vinculada a este propietario';
COMMENT ON COLUMN propietarios.codigo_vinculacion_hash IS 'SHA-256 del código de un solo uso que la clínica entrega al dueño para vincular su cuenta';

CREATE UNIQUE INDEX ux_propietarios_email ON propietarios (lower(email));
CREATE INDEX idx_propietarios_nombre ON propietarios (lower(nombre));

CREATE TABLE mascotas (
    id               UUID PRIMARY KEY,
    propietario_id   UUID         NOT NULL REFERENCES propietarios (id),
    nombre           VARCHAR(100) NOT NULL,
    especie          VARCHAR(20)  NOT NULL
        CHECK (especie IN ('PERRO', 'GATO', 'AVE', 'CONEJO', 'ROEDOR', 'REPTIL', 'OTRO')),
    raza             VARCHAR(100),
    sexo             VARCHAR(20)  NOT NULL CHECK (sexo IN ('MACHO', 'HEMBRA', 'DESCONOCIDO')),
    fecha_nacimiento DATE,
    peso_kg          NUMERIC(6, 2) CHECK (peso_kg > 0),
    color            VARCHAR(60),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_mascotas_propietario ON mascotas (propietario_id);
