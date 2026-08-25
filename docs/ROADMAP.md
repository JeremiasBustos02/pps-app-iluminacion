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

1. **Seguridad real** (hoy `SecurityConfig` tiene `permitAll()` en todos los endpoints — no hay
   autenticación ni autorización, pese a que `Usuario` ya tiene `passwordHash`).
2. **Reglas de negocio** que hoy son solo estructura de datos sin lógica: descuento automático de
   stock (RF-15), estado "espera de EDEA" con impacto en SLA (RF-17), catálogo de componentes
   rotos (RF-13), indicador de disponibilidad de repuestos (RF-18).
3. **Notificaciones** (RF-12): no hay dependencia de mail ni templates todavía.
4. **Frontend completo**: no iniciado.
5. **Reportes** (RF-21): no hay endpoints de agregación.

---

## 🔎 Trazabilidad RF → Estado de implementación

| RF | Descripción | Estado | Detalle |
|----|-------------|--------|---------|
| RF-01 | Registro de vecinos | 🟡 Parcial | `POST /api/usuarios` crea el usuario con sus datos, pero no hay flujo de "auto-registro" diferenciado de alta por Admin, ni validación de unicidad de DNI |
| RF-02 | Roles y permisos | 🟡 Parcial | `Usuario.rol` es un `String` libre; no hay enum, no hay protección de endpoints por rol |
| RF-03 | Autenticación (DNI + contraseña) | 🔴 Pendiente | Existe `passwordHash` en la entidad pero no hay login, ni JWT, ni filtro de seguridad. `SecurityConfig` permite todo sin token |
| RF-04 | Alta/baja/edición de usuarios (Admin) | 🟡 Parcial | CRUD completo con soft delete (`deletedAt`), pero sin restricción de rol ni validación de asignación de Cuadrilla al crear un Técnico |
| RF-05 | Mapa de luminarias con semáforo de estado | 🟡 Parcial (solo datos) | `LuminariaResponseDTO` expone lat/lon, zona y estado; **no existe** cálculo de color según reclamos activos, ni ocultamiento de datos técnicos para el rol Vecino, ni marca gris para zonas no urbanas |
| RF-06 | Alta de nuevos puntos de luz | 🟢 Backend listo | `POST /api/luminarias` con `LuminariaDTO` (coordenadas, tipo, zona). Falta restricción a rol Admin |
| RF-07 | Creación de reclamo por tipificación | 🟢 Completo | `ReclamoService.create()` + catálogo `tipo_reclamo` seedeado con las 7 opciones exigidas |
| RF-08 | Número de seguimiento único | 🟢 Completo | Secuencia `reclamo_numero_seq` + formato `REC-YYYY-NNNNN`, búsqueda por `GET /api/reclamos/seguimiento/{n}` |
| RF-09 | Prioridad automática por tipo | 🟢 Completo (dato) | `TipoReclamo.prioridad` seedeado; falta que el color del mapa (RF-05) y el orden de cola de trabajo lo usen |
| RF-10 | Tiempo estimado de resolución | 🟡 Simplificado | `calcularTiempoEstimado()` es un switch fijo por prioridad (24/72/168 h); **no considera** carga de cuadrillas ni zona, y no se recalcula al pasar a estado EDEA (RF-17) |
| RF-11 | Panel "Paquete de Reclamo" | 🔴 Pendiente | No existe endpoint que agregue reporte del vecino + tipificación + prioridad + estado + observaciones de cuadrilla en una sola vista |
| RF-12 | Devolución/notificación al vecino | 🔴 Pendiente | Sin dependencia de Mail, sin templates Thymeleaf, sin trigger al cerrar reclamo |
| RF-13 | Diagnóstico técnico (componente roto) | 🟢 Completo | `Reparacion` solo tiene `observacion` y `fecha`; falta entidad/catálogo `Componente` y su relación con `Reparacion` |
| RF-14 | Observaciones del técnico | 🟢 Completo | `Reparacion.observacion` + `POST /api/reparaciones` |
| RF-15 | Descuento automático de stock | 🟢 Completo | `MovimientoStockService.registrarMovimiento()` solo persiste el movimiento; no impacta `Material.cantidad`. Falta enlazar `ReparacionMaterial` → descuento real |
| RF-16 | Alta y reposición de stock | 🟢 Completo | `PATCH /api/materiales/{id}/stock` permite fijar cantidad manualmente, pero no diferencia tipo de movimiento (ingreso/egreso) ni queda registrado como `MovimientoStock` automáticamente |
| RF-17 | Estado "Espera de conexión / Alta EDEA" | 🔴 Pendiente | `Reclamo.estado` es `String` libre sin máquina de estados; no hay pausa de SLA ni recálculo de tiempo estimado |
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
* [x] Hash de contraseñas con BCrypt al dar de alta un usuario (hoy `passwordHash` se recibe tal cual en el `POST`, sin encriptar)
* [ ] Endpoint de login (`POST /api/auth/login`) validando DNI + contraseña
* [ ] Emisión de JWT y configuración de expiración
* [ ] Filtro de seguridad (`OncePerRequestFilter`) que reemplace el `permitAll()` actual
* [ ] Recuperación / cambio de contraseña

**Roles**
* [x] Convertir `Usuario.rol` en enum (`VECINO`, `TECNICO`, `ADMINISTRADOR`)
* [ ] Protección de endpoints por rol con `@PreAuthorize`
* [ ] Reglas: solo Admin da de alta Técnicos/Administradores; Vecino se auto-registra (RF-01/RF-04)

**Administración de usuarios**
* [ ] Validar unicidad de DNI (además del email ya validado)
* [ ] Validar que solo usuarios `TECNICO` puedan asociarse a una `Cuadrilla` (ya listado en el roadmap original, sigue pendiente)
* [ ] Evitar técnicos duplicados en una cuadrilla (`CuadrillaTecnicoService` ya valida esto — falta test/documentación)

---

### Fase 2 — Reclamos (RF-07 a RF-11, RF-17, RF-18)

* [x] Crear reclamo, asociar luminaria/tipo, generar número de seguimiento y tiempo estimado
* [ ] Asociar automáticamente el usuario autenticado al reclamo (sale del JWT, no del body — depende de Fase 1)
* [ ] Consultar reclamos propios (Vecino) e impedir ver reclamos de terceros
* [ ] Filtros: por estado, zona, prioridad
* [ ] Definir máquina de estados de `Reclamo` (PENDIENTE → ASIGNADO → EN_REPARACION → ESPERA_EDEA → RESUELTO → CERRADO)
* [ ] Estado "Espera de conexión / Alta por EDEA": pausa de SLA + recálculo de tiempo estimado (RF-17)
* [ ] Refinar `calcularTiempoEstimado()` para considerar carga de cuadrillas y zona, no solo prioridad
* [ ] Endpoint "Paquete de Reclamo" (RF-11): reporte del vecino + tipificación + prioridad + estado + observaciones técnicas en una sola respuesta
* [ ] Indicador de disponibilidad de materiales en el paquete de reclamo (RF-18), sin exponer stock al Vecino
* [ ] Historial de estados del reclamo (tabla de auditoría o eventos)
* [ ] Notificación al vecino ante cada cambio relevante de estado

---

### Fase 3 — Cuadrillas y hojas de ruta (RF-19, RF-20)

**Cuadrillas** ✅ mayormente completo
* [x] CRUD, soft delete, asignación/eliminación de técnicos
* [ ] Validar rol `TECNICO` al asignar (bloqueado por Fase 1 — roles)

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
* [ ] Filtrar luminarias por zona (backend ya soporta `GET /api/luminarias/zona/{id}`)
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

* [ ] Tests unitarios de Services
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
| Autenticación y roles      |       🔴 |
| Usuarios (ABM)             |       🟡 |
| Reclamos (creación)        |       🟢 |
| Reclamos (estados/SLA/EDEA)|       🔴 |
| Cuadrillas                 |       🟢 |
| Hojas de ruta               |       🟡 |
| Reparaciones (diagnóstico) |       🟢 |
| Stock (descuento auto)     |       🟢 |
| Notificaciones             |       🔴 |
| Mapas (frontend)           |       🔴 |
| Reportes                   |       🔴 |
| Frontend general           |       🔴 |
| Testing                    |       🔴 |
| Deploy                     |       🔴 |

### Referencia
🟢 Completado · 🟡 En progreso / parcial · 🔴 Pendiente

---

## 🚀 Próximos objetivos priorizados

1. **Seguridad**: BCrypt + login + JWT + roles — desbloquea todo lo demás (asociación automática de
   usuario a reclamo, restricción de endpoints, hoja de ruta "del técnico logueado").
2. **Máquina de estados del reclamo**, incluyendo "Espera de EDEA" y su impacto en el tiempo estimado.s
3. **Panel "Paquete de Reclamo"** (RF-11), que es el punto de encuentro de casi toda la información ya
   modelada.
4. Iniciar el **frontend** (mapa + flujo de creación de reclamo del vecino), que es lo primero demostrable
   de punta a punta.
5. Notificaciones y reportes, una vez estabilizado el flujo core.