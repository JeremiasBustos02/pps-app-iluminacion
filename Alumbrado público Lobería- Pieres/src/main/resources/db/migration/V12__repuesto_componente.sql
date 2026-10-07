-- V12__repuesto_componente.sql
-- RF-18: cada componente del catálogo se asocia al material de stock que lo repone,
-- para indicar al diagnosticar si el repuesto está disponible.
ALTER TABLE componente ADD COLUMN material_id BIGINT REFERENCES material(id);
