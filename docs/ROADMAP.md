# Roadmap — Sistema de Alumbrado Público (Municipalidad de Lobería)

## 🎯 Objetivo

Desarrollar un sistema web para la gestión integral de reclamos de alumbrado público, permitiendo a
vecinos registrar y consultar reclamos, y a técnicos y administradores gestionar su resolución, en línea
con la Especificación de Requisitos Funcionales (RF-01 a RF-21).

---

## 📌 Resumen ejecutivo del estado actual

El backend tiene resuelta la base de datos (PostGIS + Flyway), el modelo de dominio completo (13
entidades) y CRUDs funcionales para casi todos los recursos. Lo que falta para pasar de "prototipo
funcional" a "sistema alineado con los RF" es, en este orden:

1. ~~**Seguridad real**~~ ✅ Implementado: JWT + `@PreAuthorize` + `GlobalExceptionHandler`. Login con DNI/contraseña, BCrypt, filtro de autenticación.
2. **Reglas de negocio pendientes**: validación de transiciones de estado completada, falta pausa de SLA en estado ESPERA_EDEA (RF-17) y recálculo de tiempo estimado.
3. **Notificaciones** (RF-12): no hay dependencia de mail ni templates todavía.
4. **Frontend completo**: no iniciado.
5. **Reportes** (RF-21): no hay endpoints de agregación.

---

## 🔎 Trazabilidad RF → Estado de implementación

| RF | Descripción | Estado | Detalle |
|----|-------------|--------|---------|
| RF-01 | Registro de vecinos | 🟢 Completo | `POST /api/auth/registro` auto-registra vecinos (fuerza rol VECINO). Validación de unicidad de DNI y email. Contraseña hasheada con BCrypt |
| RF-02 | Roles y permisos | 🟢 Completo | `Usuario.rol` es enum (`VECINO`, `TECNICO`, `ADMINISTRADOR`). Endpoints protegidos con `@PreAuthorize`. `GlobalExceptionHandler` traduce errores a 400/401/403 |
| RF-03 | Autenticación (DNI + contraseña) | 🟢 Completo | Login con `POST /api/auth/login` (DNI + contraseña). JWT firmado con HS256, filtro `JwtAuthenticationFilter`, `SecurityConfig` protege todos los endpoints excepto `/api/auth/**`. DNI obligatorio y único (migración V8) |
| RF-04 | Alta/baja/edición de usuarios (Admin) | 🟢 Completo | CRUD con soft delete, restringido a ADMINISTRADOR con `@PreAuthorize`. Validación de unicidad de DNI y email. Validación de rol TECNICO al asignar a cuadrilla |
| RF-05 | Mapa de luminarias con semáforo de estado | 🟡 Parcial (solo datos) | `LuminariaResponseDTO` expone lat/lon, zona y estado; **no existe** cálculo de color según reclamos activos, ni ocultamiento de datos técnicos para el rol Vecino, ni marca gris para zonas no urbanas |
| RF-06 | Alta de nuevos puntos de luz | 🟢 Backend listo | `POST /api/luminarias` con `LuminariaDTO` (coordenadas, tipo, zona). Falta restricción a rol Admin |
| RF-07 | Creación de reclamo por tipificación | 🟢 Completo | `ReclamoService.create()` + catálogo `tipo_reclamo` seedeado con las 7 opciones exigidas |
| RF-08 | Número de seguimiento único | 🟢 Completo | Secuencia `reclamo_numero_seq` + formato `REC-YYYY-NNNNN`, búsqueda por `GET /api/reclamos/seguimiento/{n}` |
| RF-09 | Prioridad automática por tipo | 🟢 Completo (dato) | `TipoReclamo.prioridad` seedeado; falta que el color del mapa (RF-05) y el orden de cola de trabajo lo usen |
| RF-10 | Tiempo estimado de resolución | 🟡 Simplificado | `calcularTiempoEstimado()` es un switch fijo por prioridad (24/72/168 h); **no considera** carga de cuadrillas ni zona, y no se recalcula al pasar a estado EDEA (RF-17) |
| RF-11 | Panel "Paquete de Reclamo" | 🟢 Completo | `GET /api/reclamos/{id}/paquete` devuelve datos del reclamo + vecino (nombre, DNI, email) + tipo con prioridad + luminaria + historial de estados en una sola respuesta (`ReclamoPaqueteDTO`) |
| RF-12 | Devolución/notificación al vecino | 🔴 Pendiente | Sin dependencia de Mail, sin templates Thymeleaf, sin trigger al cerrar reclamo |
| RF-13 | Diagnóstico técnico (componente roto) | 🟢 Completo | `Reparacion` solo tiene `observacion` y `fecha`; falta entidad/catálogo `Componente` y su relación con `Reparacion` |
| RF-14 | Observaciones del técnico | 🟢 Completo | `Reparacion.observacion` + `POST /api/reparaciones` |
| RF-15 | Descuento automático de stock | 🟢 Completo | `MovimientoStockService.registrarMovimiento()` solo persiste el movimiento; no impacta `Material.cantidad`. Falta enlazar `ReparacionMaterial` → descuento real |
| RF-16 | Alta y reposición de stock | 🟢 Completo | `PATCH /api/materiales/{id}/stock` permite fijar cantidad manualmente, pero no diferencia tipo de movimiento (ingreso/egreso) ni queda registrado como `MovimientoStock` automáticamente |
| RF-17 | Estado "Espera de conexión / Alta EDEA" | 🟡 Parcial | `Reclamo.estado` es enum `EstadoReclamo` con validación de transiciones y historial (`ReclamoHistorial`). Falta pausa de SLA y recálculo de tiempo estimado al entrar/salir de ESPERA_EDEA |
| RF-18 | Indicador de disponibilidad de materiales | 🟢 Completo | Depende de RF-13 y RF-15 |
| RF-19 | Creación de hoja de ruta | 🟡 Parcial | CRUD de `HojaDeRuta` y de `HojaDeRutaReclamo` existen, pero agregar varios reclamos requiere múltiples llamadas (no hay endpoint de alta masiva) |
| RF-20 | Visualización/actualización de hoja de ruta por Técnico | 🟡 Parcial | Endpoints de lectura y `PATCH /api/reclamos/{id}/estado` existen; falta que ese cambio dispare automáticamente el flujo de RF-13/RF-14 |
| RF-21 | Panel de reportes e indicadores | 🔴 Pendiente | No hay endpoints de agregación (por zona, tiempo promedio, materiales consumidos) |

**Leyenda:** 🟢 Completo · 🟡 Parcial / simplificado · 🔴 Pendiente

---

## 🗺️ Roadmap por fases

### Fase 0 — Infraestructura y datos ✅

* [x] PostgreSQL + PostGIS + Flyway
* [x] Migración SHP → GeoJSON (ogr2ogr) y reproyección a EPSG:4326
* [x] Zonas creadas y asociadas a luminarias
* [x] Estructura inicial del backend y CRUDs base

---

### Fase 1 — Backend, autenticación y roles (RF-01 a RF-04)

**Usuarios y autenticación**
* [x] Hash de contraseñas con BCrypt al dar de alta un usuario
* [x] Endpoint de login (`POST /api/auth/login`) validando DNI + contraseña
* [x] Emisión de JWT con `JwtService` y configuración de expiración
* [x] Filtro de seguridad (`JwtAuthenticationFilter` / `OncePerRequestFilter`) que protege todos los endpoints
* [ ] Recuperación / cambio de contraseña

**Roles**
* [x] Convertir `Usuario.rol` en enum (`VECINO`, `TECNICO`, `ADMINISTRADOR`)
* [x] Protección de endpoints por rol con `@PreAuthorize`
* [x] Reglas: solo Admin da de alta Técnicos/Administradores; Vecino se auto-registra vía `/api/auth/registro`

**Administración de usuarios**
* [x] Validar unicidad de DNI (además del email ya validado). DNI obligatorio (`NOT NULL`) y único (migración V8)
* [x] Validar que solo usuarios `TECNICO` puedan asociarse a una `Cuadrilla`
* [x] Evitar técnicos duplicados en una cuadrilla (`CuadrillaTecnicoService` valida duplicados)

---

### Fase 2 — Reclamos (RF-07 a RF-11, RF-17, RF-18)

* [x] Crear reclamo, asociar luminaria/tipo, generar número de seguimiento y tiempo estimado
* [ ] Asociar automáticamente el usuario autenticado al reclamo (sale del JWT, no del body — depende de Fase 1)
* [ ] Consultar reclamos propios (Vecino) e impedir ver reclamos de terceros
* [x] Filtros: por estado, zona y tipo de reclamo en `GET /api/reclamos` (query params opcionales combinables con `LEFT JOIN`)
* [x] Definir máquina de estados de `Reclamo` con enum `EstadoReclamo` y validación de transiciones
* [ ] Estado "Espera de conexión / Alta por EDEA": pausa de SLA + recálculo de tiempo estimado (RF-17)
* [ ] Refinar `calcularTiempoEstimado()` para considerar carga de cuadrillas y zona, no solo prioridad
* [x] Endpoint "Paquete de Reclamo" (RF-11): `GET /api/reclamos/{id}/paquete` con `ReclamoPaqueteDTO` (vecino + tipo + prioridad + luminaria + historial)
* [ ] Indicador de disponibilidad de materiales en el paquete de reclamo (RF-18), sin exponer stock al Vecino
* [x] Historial de estados del reclamo (tabla de auditoría o eventos)
* [ ] Notificación al vecino ante cada cambio relevante de estado

---

### Fase 3 — Cuadrillas y hojas de ruta (RF-19, RF-20)

**Cuadrillas** ✅ mayormente completo
* [x] CRUD, soft delete, asignación/eliminación de técnicos
* [x] Validar rol `TECNICO` al asignar a cuadrilla

**Hojas de ruta**
* [x] CRUD básico de `HojaDeRuta` y `HojaDeRutaReclamo`
* [ ] Endpoint de alta masiva: agrupar varios reclamos pendientes en una hoja de ruta de una sola vez (RF-19)
* [ ] Vista "hoja de ruta del día" filtrada por cuadrilla/técnico autenticado (RF-20)
* [ ] Al marcar un reclamo como atendido desde la hoja de ruta, disparar el flujo de diagnóstico (RF-13) y observaciones (RF-14)

---

### Fase 4 — Reparaciones y stock (RF-13 a RF-16, RF-18)

**Reparaciones**
* [x] Registrar reparación con observación y fecha
* [x] Asociar técnicos a la reparación
* [x] Catálogo de componentes (`lámpara`, `fotocontrol`, `balasto`, `cableado`, etc.) — nueva entidad `Componente`
* [x] Relacionar `Reparacion` con el componente diagnosticado como roto (RF-13)
* [x] Asociar reparación al reclamo que la originó (hoy `Reparacion` no tiene FK a `Reclamo`)

**Materiales / Stock**
* [x] CRUD de materiales
* [x] Registrar consumo de materiales por reparación (`ReparacionMaterial`)
* [x] Registrar movimientos de stock (`MovimientoStock`) como registro histórico
* [x] **Descuento automático real**: que registrar un `ReparacionMaterial` genere un `MovimientoStock` de tipo egreso y reste de `Material.cantidad` (RF-15)
* [x] Distinguir movimientos de tipo ingreso/egreso al reponer stock (RF-16)
* [x] Indicador de stock insuficiente al diagnosticar una rotura (RF-18)

---

### Fase 5 — Frontend (no iniciado)

**Base**
* [ ] Inicializar React + TypeScript + Tailwind CSS
* [ ] Routing y manejo de sesión/JWT (depende de Fase 1)

**Vecino**
* [ ] Registro y login
* [ ] Crear reclamo seleccionando luminaria en el mapa (RF-07)
* [ ] Consultar estado y número de seguimiento propios (RF-08, RF-10)

**Técnico**
* [ ] Ver hoja de ruta del día (RF-20)
* [ ] Diagnóstico de reparación: componente roto + observaciones + materiales usados (RF-13, RF-14, RF-15)

**Administrador**
* [ ] Mapa con semáforo de estado (RF-05) y alta de puntos (RF-06)
* [ ] Gestión de usuarios, cuadrillas, hojas de ruta, stock
* [ ] Panel de reportes (RF-21)

---

### Fase 6 — Mapas y geolocalización (RF-05, RF-06)

* [ ] Integrar Leaflet + react-leaflet
* [ ] Renderizar luminarias con color según estado/prioridad de reclamos activos (RF-05)
* [ ] Marca gris para puntos fuera de Área Urbana (Napaleufú, Paraje Dos Naciones)
* [ ] Detalle por clic con datos técnicos (LED/Halógeno) visible solo para Técnico/Admin
* [ ] Alta de luminaria haciendo clic en el mapa (RF-06)
* [x] Filtrar luminarias por estado y zona con query params en `GET /api/luminarias`
* [ ] Optimizar consultas espaciales con índices GIST (ya existe `idx_luminaria_coordenadas`)

---

### Fase 7 — Notificaciones (RF-12)

* [ ] Agregar dependencia `spring-boot-starter-mail`
* [ ] Templates con Thymeleaf (alta de reclamo, cambio de estado, resolución)
* [ ] Trigger automático al cerrar un reclamo → notificación al vecino
* [ ] Selección de canal (email/WhatsApp) según medio de contacto registrado (RF-01)

---

### Fase 8 — Reportes (RF-21)

* [ ] Endpoint: reclamos agrupados por zona
* [ ] Endpoint: tiempo promedio de resolución (general y por tipo)
* [ ] Endpoint: reparaciones realizadas por período/cuadrilla
* [ ] Endpoint: consumo/inversión en materiales
* [ ] Dashboard administrativo consumiendo estos endpoints

---

### Fase 9 — Testing y calidad

* [x] Tests unitarios de Services (`UsuarioServiceTest`, `JwtServiceTest`, `CustomUserDetailsServiceTest`, `AuthControllerTest`)
* [ ] Tests de Controllers (incluyendo autorización por rol)
* [ ] Tests de integración (Reclamo → Reparación → Stock end-to-end)
* [ ] Documentación con Swagger/OpenAPI
* [ ] Validación de endpoints con Postman

---

### Fase 10 — Deploy

* [ ] Variables de entorno (credenciales DB, JWT secret, mail)
* [ ] Dockerizar backend y frontend
* [ ] CI/CD
* [ ] Dominio + HTTPS
* [ ] Monitoreo y logs

---

## 📊 Progreso

| Área                      | Progreso |
| -------------------------- | -------: |
| Infraestructura / Datos    |       🟢 |
| Autenticación y roles      |       🟢 |
| Usuarios (ABM)             |       🟢 |
| Reclamos (creación)        |       🟢 |
| Reclamos (filtros)         |       🟢 |
| Reclamos (paquete RF-11)   |       🟢 |
| Reclamos (estados/SLA/EDEA)|       🟡 |
| Cuadrillas                 |       🟢 |
| Hojas de ruta               |       🟡 |
| Reparaciones (diagnóstico) |       🟢 |
| Stock (descuento auto)     |       🟢 |
| Notificaciones             |       🔴 |
| Mapas (frontend)           |       🔴 |
| Reportes                   |       🔴 |
| Frontend general           |       🔴 |
| Testing                    |       🟡 |
| Deploy                     |       🔴 |

### Referencia
🟢 Completado · 🟡 En progreso / parcial · 🔴 Pendiente

---

## 🚀 Próximos objetivos priorizados

1. ~~**Seguridad**~~ ✅ Completado.
2. ~~**Paquete de Reclamo (RF-11)**~~ ✅ Completado.
3. **Pausa de SLA en ESPERA_EDEA** (RF-17): pausar/recalcular tiempo estimado al entrar/salir de este estado.
4. **Alta masiva en hoja de ruta** (RF-19): agrupar varios reclamos en una hoja de una sola vez.
5. Iniciar el **frontend** (mapa + flujo de creación de reclamo del vecino), que es lo primero demostrable de punta a punta.
6. Notificaciones y reportes, una vez estabilizado el flujo core.