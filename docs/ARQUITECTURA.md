# Arquitectura — Sistema de Gestión y Mantenimiento de Alumbrado Público

Municipalidad de Lobería

---

## 1. Visión general

El sistema sigue una arquitectura en capas clásica de aplicación web, con un backend REST en Spring
Boot, persistencia relacional con soporte geoespacial (PostGIS) y un frontend SPA (React) aún no
iniciado que consumirá la API vía HTTP/JSON.

```
┌─────────────────────────────────────────────────────────┐
│                        Frontend                         │
│         React + TypeScript + Tailwind CSS               │
│         Leaflet / react-leaflet (mapa interactivo)      │
└───────────────────────────┬─────────────────────────────┘
                             │ HTTPS / JSON (REST)
┌───────────────────────────▼─────────────────────────────┐
│                         Backend                         │
│  ┌───────────────┐  ┌───────────────┐  ┌──────────────┐ │
│  │  Controller   │→ │   Service     │→ │  Repository  │ │
│  │  (REST, DTOs) │  │ (reglas de    │  │ (Spring Data │ │
│  │               │  │  negocio)     │  │   JPA)       │ │
│  └───────────────┘  └───────────────┘  └──────┬───────┘ │
│  Spring Security (JWT) — pendiente de implementar       │
│  Spring Boot Starter Mail + Thymeleaf — pendiente       │
└─────────────────────────────────────────────────────────┘
                             │ JDBC
┌───────────────────────────▼─────────────────────────────┐
│                PostgreSQL + PostGIS                     │
│         Migraciones versionadas con Flyway              │
└─────────────────────────────────────────────────────────┘
                             ▲
                             │ ogr2ogr (una vez, carga inicial)
                    Datos SHP/QGIS del POT municipal
```

---

## 2. Stack tecnológico

| Capa | Tecnología | Estado en el repo |
|------|-----------|--------------------|
| Frontend | React + TypeScript + Tailwind CSS | No iniciado |
| Backend | Java + Spring Boot + Spring Data JPA | Implementado (controllers, services, repositories) |
| Seguridad | Spring Security + JWT | Configurado como `permitAll()` — **sin autenticación real** |
| Mapa | Leaflet + react-leaflet | No iniciado (backend expone lat/lon vía DTO) |
| Base de datos | PostgreSQL + PostGIS | Implementado, columna `geometry(Point,4326)` en `luminaria` |
| Migración de esquema | Flyway | Implementado (`V1`…`V4` vistas en el repo) |
| Migración desde QGIS | ogr2ogr | Proceso de carga inicial (fuera del backend, ya ejecutado) |
| Servicio de mail | Spring Boot Starter Mail + Thymeleaf | No iniciado |

---

## 3. Estructura de paquetes (backend)

```
com/
├── controller/    → Endpoints REST, un controller por recurso (13 controllers)
├── dto/           → Objetos de entrada/salida que no exponen la entidad JPA directamente
│                    (LuminariaDTO, LuminariaResponseDTO, ReclamoRequest, ReclamoResponse,
│                     CuadrillaTecnicoRequest)
├── entity/        → Modelo de dominio JPA (13 entidades)
├── repository/    → Interfaces Spring Data JPA
├── service/       → Reglas de negocio y orquestación entre repositorios
└── security/      → SecurityConfig (hoy: permitAll, sin filtro JWT)
```

**Convención observada:** los recursos "simples" (Zona, Material, TipoReclamo, Usuario) exponen la
entidad directamente en el controller. Los recursos con lógica de mapeo geográfico o de negocio
(Luminaria, Reclamo, CuadrillaTecnico) usan DTOs de request/response dedicados. Se recomienda
mantener este criterio a medida que se agreguen entidades nuevas (p. ej. `Componente` para RF-13).

---

## 4. Modelo de datos

### 4.1 Entidades y responsabilidad

| Entidad | Responsabilidad | Soft delete |
|---------|------------------|:-----------:|
| `Usuario` | Vecino / Técnico / Administrador. Rol como `String` libre (pendiente enum) | ✅|
| `Zona` | Agrupación geográfica de luminarias (Lobería, Tamangueyú, San Manuel, etc.) | ✅ |
| `Luminaria` | Punto de luz georreferenciado (`geometry(Point,4326)`), tipo, estado, zona | ✅ |
| `TipoReclamo` | Catálogo de tipificaciones con `prioridad` asociada (RF-07/RF-09) | ❌ |
| `Reclamo` | Reporte del vecino: número de seguimiento, estado, tiempo estimado | ❌ |
| `Cuadrilla` | Equipo de trabajo técnico | ✅ |
| `CuadrillaTecnico` | Relación N:M Usuario–Cuadrilla (con validación de no duplicados) | ❌ |
| `HojaDeRuta` | Plan de trabajo diario de una cuadrilla | ❌ |
| `HojaDeRutaReclamo` | Relación N:M HojaDeRuta–Reclamo | ❌ |
| `Reparacion` | Registro de intervención técnica (observación, fecha) | ❌ |
| `ReparacionTecnico` | Relación N:M Reparacion–Usuario (técnicos que intervinieron) | ❌ |
| `Material` | Ítem de inventario con `cantidad` | ✅ |
| `ReparacionMaterial` | Materiales consumidos en una reparación | ❌ |
| `MovimientoStock` | Historial de movimientos de stock (ingreso/egreso) | ❌ |

### 4.2 Relaciones principales

```
Zona 1───N Luminaria 1───N Reclamo N───1 TipoReclamo
                                │
                                N
                                │
                          HojaDeRutaReclamo
                                │
                                1
                                │
                          HojaDeRuta N───1 Cuadrilla 1───N CuadrillaTecnico N───1 Usuario

Reparacion N───1 ReparacionTecnico N───1 Usuario
Reparacion N───1 ReparacionMaterial N───1 Material
Material 1───N MovimientoStock N───1 Reparacion (opcional)
```

### 4.3 Brechas del modelo respecto a los RF

- **`Reparacion` no tiene FK a `Reclamo`.** Hoy no hay forma directa de saber qué reparación cerró
  qué reclamo; debería agregarse `reclamo_id` a `reparacion` para soportar RF-11 (Paquete de
  Reclamo) y RF-20.
- **Falta entidad `Componente`** (lámpara, fotocontrol, balasto, cableado) para el diagnóstico técnico
  de RF-13. Actualmente `Reparacion` no registra qué se rompió, solo una observación de texto libre.
- **`Reclamo.estado` y `Usuario.rol` son `String` libres**, sin enum ni constraint a nivel de base de
  datos. Recomendado migrar a tipos enumerados (Java `enum` + `CHECK` constraint o tabla catálogo)
  antes de construir la máquina de estados de RF-17.
- **No hay tabla de auditoría/historial de estados del reclamo**, necesaria para trazabilidad y para el
  panel de reportes (RF-21).

---

## 5. Seguridad (estado actual y objetivo)

**Estado actual:** `SecurityConfig` deshabilita CSRF y aplica `anyRequest().permitAll()`. No hay
autenticación, no hay JWT, cualquier cliente puede invocar cualquier endpoint. Esto es aceptable solo
para el estadio de pruebas con Postman descripto en el propio código, **no para producción**.

**Objetivo (Fase 1 del roadmap):**

1. Hash de contraseñas con BCrypt en el alta de `Usuario`.
2. Endpoint `POST /api/auth/login` que valide DNI + contraseña y devuelva un JWT.
3. Filtro `OncePerRequestFilter` que valide el JWT en cada request y setee el `Authentication` en el
   `SecurityContext`.
4. Reglas de autorización por rol con `@PreAuthorize` o configuración declarativa en
   `SecurityFilterChain`, por ejemplo:
   - `VECINO`: puede crear reclamos y leer solo los propios.
   - `TECNICO`: puede leer su hoja de ruta y registrar reparaciones/materiales.
   - `ADMINISTRADOR`: acceso total, incluyendo ABM de usuarios, cuadrillas y stock.
5. El `usuario_id` de un `Reclamo` debe completarse a partir del JWT (no del body del request), tal
   como ya está anotado como pendiente en `ReclamoService`.

---

## 6. Módulo geoespacial

- `Luminaria.coordenadas` es una columna `geometry(Point,4326)` (WGS84), con índice `GIST` para
  consultas espaciales eficientes.
- La carga inicial de datos proviene del Plan de Ordenamiento Territorial (shapefiles de QGIS),
  convertidos a GeoJSON y reproyectados con `ogr2ogr` como paso de migración único, fuera del ciclo
  de vida normal de la aplicación.
- El frontend (Leaflet/react-leaflet, no iniciado) consumirá `LuminariaResponseDTO`, que ya expone
  `latitud`/`longitud` planos en lugar de la geometría JTS cruda, evitando acoplar el cliente a PostGIS.
- Pendiente: lógica de color/semáforo (RF-05) — hoy es responsabilidad exclusiva del frontend, ya que
  el backend no calcula un "estado agregado" de la luminaria en base a sus reclamos activos. Hay que evaluar exponer ese cálculo desde el backend (por ejemplo, un campo derivado en
  `LuminariaResponseDTO`) para no duplicar la regla de prioridad en el cliente.

---

## 7. Flujo de negocio principal (reclamo → resolución)

```
Vecino crea reclamo (RF-07)
        │
        ▼
Sistema genera número de seguimiento (RF-08) y prioridad/tiempo estimado (RF-09/RF-10)
        │
        ▼
Admin agrupa reclamos pendientes en una Hoja de Ruta por Cuadrilla (RF-19)
        │
        ▼
Técnico visualiza su Hoja de Ruta (RF-20)
        │
        ├── ¿Depende de EDEA? → estado ESPERA_EDEA, se pausa SLA (RF-17)
        │
        ▼
Técnico diagnostica componente roto (RF-13) y carga observaciones (RF-14)
        │
        ▼
Sistema descuenta stock automáticamente (RF-15) y verifica disponibilidad (RF-18)
        │
        ▼
Reclamo se marca RESUELTO/CERRADO
        │
        ▼
Notificación automática al vecino (RF-12)
```

Este flujo es el hilo conductor del roadmap: hoy están resueltos los primeros dos pasos (creación del
reclamo) y de forma aislada algunos pasos intermedios (hoja de ruta, reparación), pero falta la
orquestación end-to-end (máquina de estados, descuento de stock, notificación).

---

## 8. Consideraciones de despliegue

- Variables de entorno para credenciales de base de datos, secreto JWT y configuración SMTP —
  nunca hardcodeadas.
- Pipeline de CI/CD que corra las migraciones Flyway antes de desplegar la nueva versión del
  backend.