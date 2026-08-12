# Sistema de Gestión y Mantenimiento de Alumbrado Público

Municipalidad de Lobería

Sistema web para centralizar la gestión de luminarias públicas, permitiendo a los vecinos reportar
fallas directamente sobre un mapa interactivo y a las cuadrillas técnicas y al área de administración
gestionar el ciclo completo de reclamos, reparaciones y stock de materiales.

> 📄 Requisitos funcionales completos: ver `RF-01` a `RF-21` en la especificación del proyecto.
> 🗺️ Estado de avance y próximos pasos: [`roadmap.md`](./roadmap.md)
> 🏗️ Diseño técnico del sistema: [`arquitectura.md`](./arquitectura.md)
> 🔌 Referencia de endpoints: [`api.md`](./api.md)

---

## ⚠️ Estado del proyecto

Este proyecto está **en desarrollo activo**. El backend tiene resueltos los CRUDs principales y la
carga geoespacial inicial, pero **todavía no tiene autenticación real** (todos los endpoints están
abiertos vía `permitAll()`) ni frontend. Antes de usar este sistema en producción, revisar la sección
"Próximos objetivos priorizados" de [`roadmap.md`](./roadmap.md).

---

## 🧱 Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Frontend | React + TypeScript + Tailwind CSS *(no iniciado)* |
| Backend | Java + Spring Boot + Spring Data JPA + Spring Security (JWT) |
| Mapa | Leaflet + react-leaflet *(no iniciado)* |
| Base de datos | PostgreSQL + extensión PostGIS |
| Migración de esquema | Flyway |
| Migración de datos QGIS | ogr2ogr |
| Servicio de mail | Spring Boot Starter Mail + Thymeleaf *(no iniciado)* |

---

## 📁 Estructura del repositorio

```
.
├── src/main/java/com/
│   ├── controller/     # Endpoints REST
│   ├── dto/             # Objetos de request/response
│   ├── entity/           # Entidades JPA
│   ├── repository/       # Interfaces Spring Data JPA
│   ├── service/           # Lógica de negocio
│   └── security/           # Configuración de Spring Security
├── src/main/resources/
│   └── db/migration/        # Scripts Flyway (V1__..., V2__..., etc.)
├── roadmap.md
├── arquitectura.md
├── api.md
└── README.md
```

---

## 🚀 Puesta en marcha (backend)

### Requisitos previos

- Java 21+
- Maven o el wrapper incluido (`./mvnw`)
- PostgreSQL 18+ con la extensión **PostGIS** disponible
- (Opcional) Postman u otro cliente REST para probar los endpoints

### 1. Base de datos

Crear la base y habilitar PostGIS:

```sql
CREATE DATABASE alumbrado_db;
\c alumbrado_db
CREATE EXTENSION IF NOT EXISTS postgis;
```

Flyway se encarga del resto del esquema (tablas, secuencias y datos semilla de `tipo_reclamo` y
`zona`) al levantar la aplicación por primera vez.

### 2. Configuración

El proyecto usa `application.properties`. Estructura esperada (con placeholders):

```properties
spring.application.name=backend-alumbrado

# Configuración de Base de Datos PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/alumbrado_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate (Spring Boot 3 + PostGIS)
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true

# Flyway para migraciones automáticas de BD
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Configuración JWT (Clave con mínimo 32 caracteres aleatorios)
jwt.secret=${JWT_SECRET}
jwt.expiration=86400000
```

Para desarrollo local, `${DB_USERNAME}`/`${DB_PASSWORD}`/`${JWT_SECRET}` se resuelven con variables
de entorno o con un `application-local.properties` que **no se commitea**.

### 3. Levantar el backend

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080/api`. Ver [`api.md`](./api.md) para el detalle de
cada endpoint.

---

## 🗄️ Modelo de datos

Ver el detalle completo de entidades y relaciones en [`arquitectura.md`](./arquitectura.md#4-modelo-de-datos).
En resumen: `Usuario`, `Zona`, `Luminaria`, `TipoReclamo`, `Reclamo`, `Cuadrilla`,
`CuadrillaTecnico`, `HojaDeRuta`, `HojaDeRutaReclamo`, `Reparacion`, `ReparacionTecnico`,
`Material`, `ReparacionMaterial` y `MovimientoStock`.

---

## 🔐 Seguridad

Actualmente **no implementada** — todos los endpoints son públicos. El plan de trabajo (login con
DNI + contraseña, JWT, roles `VECINO`/`TECNICO`/`ADMINISTRADOR`) está detallado en la Fase 1 de
[`roadmap.md`](./roadmap.md#fase-1--backend-autenticación-y-roles-rf-01-a-rf-04) y en la sección de
seguridad de [`arquitectura.md`](./arquitectura.md#5-seguridad-estado-actual-y-objetivo).

---

## 🧭 Cómo contribuir / por dónde seguir

1. Revisar [`roadmap.md`](./roadmap.md) para ver qué fase está en curso.
2. Antes de agregar un endpoint nuevo, revisar [`api.md`](./api.md) para mantener las convenciones
   existentes (DTOs para recursos con mapeo geográfico o de negocio, entidad directa para recursos
   simples).
3. Cualquier cambio de modelo de datos debe ir acompañado de un script Flyway nuevo (`Vn__descripcion.sql`),
   nunca modificando uno ya aplicado.
4. Actualizar `roadmap.md` al cerrar una tarea, para mantener la trazabilidad con los RF.