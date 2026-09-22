-- V5__add_reclamo_id_to_reparacion.sql
ALTER TABLE reparacion
    ADD COLUMN reclamo_id BIGINT REFERENCES reclamo(id);