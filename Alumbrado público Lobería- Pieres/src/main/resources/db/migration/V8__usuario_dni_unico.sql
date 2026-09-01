-- V8__usuario_dni_unico.sql
-- RF-03: el DNI es la credencial de inicio de sesion, por lo que debe ser
-- obligatorio y unico. Si la base tuviera filas con dni nulo o duplicado,
-- esta migracion fallara y esos datos deben corregirse antes de reintentar.
ALTER TABLE usuario ALTER COLUMN dni SET NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_dni UNIQUE (dni);
