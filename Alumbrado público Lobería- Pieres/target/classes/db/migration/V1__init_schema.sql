-- 1. Extensión PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Zona
CREATE TABLE zona (
                      id BIGSERIAL PRIMARY KEY,
                      nombre VARCHAR(100) NOT NULL UNIQUE,
                      localidad VARCHAR(100),
                      deleted_at TIMESTAMP,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP
);

INSERT INTO zona (nombre) VALUES
                              ('Zona Norte'), ('Zona Sur'), ('Zona Este'), ('Zona Oeste'),
                              ('Zona Acceso'), ('Zona 227'), ('Zona Pieres'), ('Zona Tamangueyu');

-- 3. Usuario
CREATE TABLE usuario (
                         id BIGSERIAL PRIMARY KEY,
                         nombre VARCHAR(100) NOT NULL,
                         rol VARCHAR(50) NOT NULL, -- VECINO, TECNICO, ADMINISTRADOR
                         email VARCHAR(150) NOT NULL UNIQUE,
                         password_hash VARCHAR(255) NOT NULL,
                         dni BIGINT,
                         celular VARCHAR(50),
                         calle VARCHAR(100),
                         numero_calle INT,
                         referencia_domicilio VARCHAR(255),
                         deleted_at TIMESTAMP,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);

-- 4. Luminaria
CREATE TABLE luminaria (
                           id BIGSERIAL PRIMARY KEY,
                           potencia VARCHAR(50),
                           columna VARCHAR(50),
                           coordenadas GEOMETRY(Point, 4326) NOT NULL,
                           tipo VARCHAR(50),
                           estado VARCHAR(50),
                           zona_id BIGINT REFERENCES zona(id),
                           deleted_at TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP
);

CREATE INDEX idx_luminaria_coordenadas ON luminaria USING GIST (coordenadas);

-- 5. TipoReclamo
CREATE TABLE tipo_reclamo (
                              id BIGSERIAL PRIMARY KEY,
                              nombre VARCHAR(100) NOT NULL,
                              prioridad INT,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP
);

-- 6. Reclamo
CREATE TABLE reclamo (
                         id BIGSERIAL PRIMARY KEY,
                         numero_seguimiento VARCHAR(50) UNIQUE NOT NULL,
                         estado VARCHAR(50) DEFAULT 'PENDIENTE',
                         fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         tiempo_estimado INT,
                         luminaria_id BIGINT REFERENCES luminaria(id),
                         tipo_reclamo_id BIGINT REFERENCES tipo_reclamo(id),
                         usuario_id BIGINT REFERENCES usuario(id),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP
);

-- 7. Cuadrilla
CREATE TABLE cuadrilla (
                           id BIGSERIAL PRIMARY KEY,
                           nombre VARCHAR(100) NOT NULL,
                           deleted_at TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP
);

CREATE TABLE cuadrilla_tecnico (
                                   id BIGSERIAL PRIMARY KEY,
                                   cuadrilla_id BIGINT REFERENCES cuadrilla(id),
                                   usuario_id BIGINT REFERENCES usuario(id),
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP
);

-- 8. HojaDeRuta
CREATE TABLE hoja_de_ruta (
                              id BIGSERIAL PRIMARY KEY,
                              cuadrilla_id BIGINT REFERENCES cuadrilla(id),
                              fecha TIMESTAMP,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP
);

CREATE TABLE hoja_de_ruta_reclamo (
                                      id BIGSERIAL PRIMARY KEY,
                                      reclamo_id BIGINT REFERENCES reclamo(id),
                                      hoja_de_ruta_id BIGINT REFERENCES hoja_de_ruta(id),
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP
);

-- 9. Material
CREATE TABLE material (
                          id BIGSERIAL PRIMARY KEY,
                          nombre VARCHAR(100) NOT NULL,
                          cantidad INT DEFAULT 0,
                          deleted_at TIMESTAMP,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP
);

-- 10. Reparacion y Stock
CREATE TABLE reparacion (
                            id BIGSERIAL PRIMARY KEY,
                            observacion VARCHAR(255),
                            fecha TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
);

CREATE TABLE reparacion_tecnico (
                                    id BIGSERIAL PRIMARY KEY,
                                    reparacion_id BIGINT REFERENCES reparacion(id),
                                    usuario_id BIGINT REFERENCES usuario(id),
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP
);

CREATE TABLE reparacion_material (
                                     id BIGSERIAL PRIMARY KEY,
                                     reparacion_id BIGINT REFERENCES reparacion(id),
                                     material_id BIGINT REFERENCES material(id),
                                     cantidad INT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP
);

CREATE TABLE movimiento_stock (
                                  id BIGSERIAL PRIMARY KEY,
                                  reparacion_id BIGINT REFERENCES reparacion(id),
                                  material_id BIGINT REFERENCES material(id),
                                  tipo VARCHAR(50),
                                  cantidad INT,
                                  fecha TIMESTAMP,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP
);