CREATE TABLE duenos (
    id UUID PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    direccion VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE mascotas (
    id UUID PRIMARY KEY,
    dueno_id UUID NOT NULL REFERENCES duenos(id),
    nombre VARCHAR(150) NOT NULL,
    especie VARCHAR(80) NOT NULL,
    raza VARCHAR(100),
    sexo VARCHAR(20) NOT NULL CHECK (sexo IN ('MACHO', 'HEMBRA', 'DESCONOCIDO')),
    fecha_nacimiento DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_mascotas_dueno ON mascotas(dueno_id);
