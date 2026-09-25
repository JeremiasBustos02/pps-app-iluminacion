-- V13__precio_material.sql
-- RF-21: precio unitario del material para calcular consumo e inversión.
-- El movimiento guarda el precio vigente al momento de registrarse, así los reportes
-- históricos no cambian cuando se actualiza el precio.
ALTER TABLE material ADD COLUMN precio_unitario NUMERIC(12, 2);
ALTER TABLE movimiento_stock ADD COLUMN precio_unitario NUMERIC(12, 2);
