-- Catálogo de componentes
CREATE TABLE componente (
                            id BIGSERIAL PRIMARY KEY,
                            nombre VARCHAR(100) NOT NULL UNIQUE,
                            descripcion VARCHAR(255),
                            deleted_at TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
);

-- Relación de diagnóstico en la reparación
CREATE TABLE reparacion_componente (
                                       id BIGSERIAL PRIMARY KEY,
                                       reparacion_id BIGINT NOT NULL REFERENCES reparacion(id),
                                       componente_id BIGINT NOT NULL REFERENCES componente(id),
                                       estado_componente VARCHAR(50),
                                       observacion VARCHAR(255),
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP
);