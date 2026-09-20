-- V10__mapa_luminarias.sql
-- RF-05: marca gris para puntos fuera del Área Urbana y observación del vecino en el reclamo.
ALTER TABLE zona ADD COLUMN area_urbana BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO zona (nombre, area_urbana) VALUES
    ('Napaleufú', FALSE),
    ('Paraje Dos Naciones', FALSE)
ON CONFLICT (nombre) DO UPDATE SET area_urbana = FALSE;

ALTER TABLE reclamo ADD COLUMN observacion VARCHAR(500);
