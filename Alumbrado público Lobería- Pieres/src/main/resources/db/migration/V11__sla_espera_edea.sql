-- V11__sla_espera_edea.sql
-- RF-17: pausa del SLA mientras el reclamo espera el alta de EDEA.
ALTER TABLE reclamo ADD COLUMN fecha_limite TIMESTAMP;
ALTER TABLE reclamo ADD COLUMN sla_pausado_desde TIMESTAMP;
ALTER TABLE reclamo ADD COLUMN minutos_pausa INT NOT NULL DEFAULT 0;

-- RF-10: reclamos existentes sin tiempo estimado toman el plazo según la prioridad del tipo
UPDATE reclamo r
SET tiempo_estimado = CASE t.prioridad WHEN 3 THEN 24 WHEN 2 THEN 72 ELSE 168 END
FROM tipo_reclamo t
WHERE r.tipo_reclamo_id = t.id AND r.tiempo_estimado IS NULL;

UPDATE reclamo
SET fecha_limite = fecha + make_interval(hours => tiempo_estimado)
WHERE tiempo_estimado IS NOT NULL AND fecha IS NOT NULL;

-- Reclamos que ya están en espera de EDEA arrancan con el SLA pausado
UPDATE reclamo SET sla_pausado_desde = now() WHERE estado = 'ESPERA_EDEA';
