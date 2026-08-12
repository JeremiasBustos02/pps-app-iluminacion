ALTER TABLE reparacion
    ADD COLUMN reclamo_id BIGINT REFERENCES reclamo(id);