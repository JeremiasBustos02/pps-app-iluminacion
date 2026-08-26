-- V7__create_reclamo_historial.sql
CREATE TABLE reclamo_historial (
    id BIGSERIAL PRIMARY KEY,
    reclamo_id BIGINT NOT NULL REFERENCES reclamo(id),
    estado_anterior VARCHAR(50),
    estado_nuevo VARCHAR(50) NOT NULL,
    observacion VARCHAR(255),
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);