# API — Sistema de Gestión de Alumbrado Público

Referencia de los endpoints REST expuestos actualmente por el backend (Spring Boot). Documenta lo
que existe hoy en el código; las columnas **RF** y **Notas** señalan brechas respecto a la especificación
de requisitos.

## Convenciones generales

- **Base URL:** `/api`
- **Formato:** JSON (`Content-Type: application/json`)
- **Autenticación:** ⚠️ **No implementada todavía.** Todos los endpoints son públicos
  (`SecurityConfig` usa `permitAll()`). Cuando se implemente JWT (ver `arquitectura.md`), se espera:
  `Authorization: Bearer <token>` en cada request salvo `POST /api/auth/login` y `POST /api/usuarios`
  (auto-registro de vecino).
- **Soft delete:** los recursos con `deletedAt` (Usuario, Zona, Luminaria, Cuadrilla, Material) no se
  borran físicamente; el `DELETE` marca `deletedAt` y los `GET` los excluyen automáticamente.
- **Códigos de estado usados:** `200 OK`, `201 Created`, `204 No Content`, `404 Not Found`. No hay
  todavía manejo centralizado de errores (`@ControllerAdvice`); las excepciones de negocio
  (`RuntimeException`) hoy no se traducen a un `400`/`409` estructurado.

---

## Usuarios

Base: `/api/usuarios`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/usuarios` | — | `200` `Usuario[]` (sin eliminados) | RF-04. No filtra por rol |
| GET | `/api/usuarios/{id}` | — | `200` `Usuario` / `404` | |
| POST | `/api/usuarios` | `Usuario` (nombre, rol, email, passwordHash, dni, celular, calle, numeroCalle, referenciaDomicilio) | `201` `Usuario` | RF-01. ⚠️ `passwordHash` se persiste tal cual, **sin encriptar con BCrypt** todavía |
| PUT | `/api/usuarios/{id}` | `Usuario` (nombre, celular, calle, numeroCalle, referenciaDomicilio) | `200` `Usuario` | El rol y el email no se actualizan por este endpoint |
| DELETE | `/api/usuarios/{id}` | — | `204` | Soft delete (RF-04) |

**Pendiente:** `POST /api/auth/login` (RF-03) no existe todavía.

---

## Zonas

Base: `/api/zonas`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/zonas` | — | `200` `Zona[]` | |
| GET | `/api/zonas/{id}` | — | `200` `Zona` / `404` | |
| POST | `/api/zonas` | `Zona` (nombre) | `201` `Zona` | |
| PUT | `/api/zonas/{id}` | `Zona` (nombre) | `200` `Zona` | |
| DELETE | `/api/zonas/{id}` | — | `204` | Soft delete |

---

## Luminarias

Base: `/api/luminarias`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/luminarias` | — | `200` `LuminariaResponseDTO[]` | Incluye `latitud`/`longitud` planos y `zonaNombre` |
| GET | `/api/luminarias/{id}` | — | `200` `LuminariaResponseDTO` / `404` | |
| GET | `/api/luminarias/zona/{zonaId}` | — | `200` `LuminariaResponseDTO[]` | Soporta filtro por zona (RF-06 / mapa) |
| POST | `/api/luminarias` | `LuminariaDTO` (potencia, columna, tipo, estado, zonaId, latitud, longitud) | `201` `Luminaria` | RF-06. ⚠️ Devuelve la entidad completa, no el `ResponseDTO` |
| PUT | `/api/luminarias/{id}` | `LuminariaDTO` | `200` `Luminaria` | |
| DELETE | `/api/luminarias/{id}` | — | `204` | Soft delete |

**Pendiente:** filtro por `estado` (el repositorio `findByEstadoAndDeletedAtIsNull` existe pero no está
expuesto en el controller); cálculo de color/prioridad para el mapa (RF-05).

---

## Tipos de reclamo

Base: `/api/tipos-reclamos`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/tipos-reclamos` | — | `200` `TipoReclamo[]` | Catálogo seedeado (RF-07): Luminaria apagada, Parpadeo intermitente, Caída de rama, Corte de luz general, Poste dañado o caído, Cableado expuesto, Otro |
| GET | `/api/tipos-reclamos/{id}` | — | `200` `TipoReclamo` / `404` | |
| POST | `/api/tipos-reclamos` | `TipoReclamo` (nombre, prioridad) | `201` `TipoReclamo` | |
| DELETE | `/api/tipos-reclamos/{id}` | — | `204` | ⚠️ Hard delete (`TipoReclamo` no tiene `deletedAt`) |

---

## Reclamos

Base: `/api/reclamos`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/reclamos` | — | `200` `Reclamo[]` | Sin filtro por usuario/rol todavía (RF-04.2, pendiente) |
| GET | `/api/reclamos/{id}` | — | `200` `Reclamo` / `404` | |
| GET | `/api/reclamos/seguimiento/{numeroSeguimiento}` | — | `200` `Reclamo` / `404` | RF-08 |
| POST | `/api/reclamos` | `ReclamoRequest` (luminariaId, tipoReclamoId) | `201` `ReclamoResponse` | RF-07/RF-08/RF-09/RF-10. Genera `numeroSeguimiento`, `estado=PENDIENTE` y `tiempoEstimado` automáticamente. ⚠️ `usuarioId` queda `null` hasta que exista JWT |
| PATCH | `/api/reclamos/{id}/estado?estado=...` | — (query param) | `200` `Reclamo` | RF-20. `estado` es texto libre, sin validación contra una máquina de estados (RF-17 pendiente) |

**Pendiente:** `GET /api/reclamos/propios` (RF: consulta de reclamos propios), filtros por estado/zona/
prioridad, endpoint "Paquete de Reclamo" (RF-11).

---

## Cuadrillas

Base: `/api/cuadrillas`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/cuadrillas` | — | `200` `Cuadrilla[]` | |
| GET | `/api/cuadrillas/{id}` | — | `200` `Cuadrilla` / `404` | |
| POST | `/api/cuadrillas` | `Cuadrilla` (nombre) | `201` `Cuadrilla` | |
| PUT | `/api/cuadrillas/{id}` | `Cuadrilla` (nombre) | `200` `Cuadrilla` | |
| DELETE | `/api/cuadrillas/{id}` | — | `204` | Soft delete |

### Asignación de técnicos a cuadrillas

Base: `/api/cuadrilla-tecnicos`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/cuadrilla-tecnicos/cuadrilla/{cuadrillaId}` | — | `200` `CuadrillaTecnico[]` | |
| GET | `/api/cuadrilla-tecnicos/tecnico/{tecnicoId}` | — | `200` `CuadrillaTecnico[]` | |
| POST | `/api/cuadrilla-tecnicos` | `CuadrillaTecnicoRequest` (cuadrillaId, tecnicoId) | `201` `CuadrillaTecnico` | Rechaza duplicados (técnico ya en la cuadrilla) con `RuntimeException`. ⚠️ No valida todavía que el usuario tenga rol `TECNICO` |
| DELETE | `/api/cuadrilla-tecnicos/{id}` | — | `204` | |

---

## Hojas de ruta

Base: `/api/hojas-de-ruta`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/hojas-de-ruta` | — | `200` `HojaDeRuta[]` | |
| GET | `/api/hojas-de-ruta/{id}` | — | `200` `HojaDeRuta` / `404` | |
| POST | `/api/hojas-de-ruta` | `HojaDeRuta` (cuadrillaId, fecha) | `201` `HojaDeRuta` | RF-19 |
| DELETE | `/api/hojas-de-ruta/{id}` | — | `204` | ⚠️ Hard delete |

### Reclamos asignados a una hoja de ruta

Base: `/api/hoja-ruta-reclamos`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/hoja-ruta-reclamos/hoja/{hojaDeRutaId}` | — | `200` `HojaDeRutaReclamo[]` | RF-20 |
| POST | `/api/hoja-ruta-reclamos` | `HojaDeRutaReclamo` (reclamoId, hojaDeRutaId) | `201` `HojaDeRutaReclamo` | Un reclamo por request; no hay alta masiva todavía (RF-19) |
| DELETE | `/api/hoja-ruta-reclamos/{id}` | — | `204` | |

---

## Reparaciones

Base: `/api/reparaciones`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/reparaciones` | — | `200` `Reparacion[]` | |
| GET | `/api/reparaciones/{id}` | — | `200` `Reparacion` / `404` | |
| POST | `/api/reparaciones` | `Reparacion` (observacion, fecha) | `201` `Reparacion` | RF-14. ⚠️ No tiene `reclamoId` ni `componente` (RF-13 pendiente) |
| DELETE | `/api/reparaciones/{id}` | — | `204` | Hard delete |

### Técnicos que intervinieron

Base: `/api/reparacion-tecnicos`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/reparacion-tecnicos/reparacion/{reparacionId}` | — | `200` `ReparacionTecnico[]` | |
| POST | `/api/reparacion-tecnicos` | `ReparacionTecnico` (reparacionId, usuarioId) | `201` `ReparacionTecnico` | |

### Materiales usados en una reparación

Base: `/api/reparacion-materiales`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/reparacion-materiales/reparacion/{reparacionId}` | — | `200` `ReparacionMaterial[]` | |
| POST | `/api/reparacion-materiales` | `ReparacionMaterial` (reparacionId, materialId, cantidad) | `201` `ReparacionMaterial` | ⚠️ **No descuenta stock automáticamente** (RF-15 pendiente) |

---

## Materiales y stock

Base: `/api/materiales`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/materiales` | — | `200` `Material[]` | |
| GET | `/api/materiales/{id}` | — | `200` `Material` / `404` | |
| POST | `/api/materiales` | `Material` (nombre, cantidad) | `201` `Material` | |
| PATCH | `/api/materiales/{id}/stock?cantidad=N` | — (query param) | `200` `Material` | ⚠️ Fija el valor absoluto de `cantidad`, no lo incrementa/decrementa; no distingue ingreso de egreso (RF-16) |
| DELETE | `/api/materiales/{id}` | — | `204` | Soft delete |

Base: `/api/movimientos-stock`

| Método | Path | Body | Respuesta | Notas |
|--------|------|------|-----------|-------|
| GET | `/api/movimientos-stock` | — | `200` `MovimientoStock[]` | |
| GET | `/api/movimientos-stock/material/{materialId}` | — | `200` `MovimientoStock[]` | |
| POST | `/api/movimientos-stock` | `MovimientoStock` (reparacionId opcional, materialId, tipo, cantidad, fecha) | `201` `MovimientoStock` | Solo registra el movimiento; **no actualiza `Material.cantidad`** (queda desincronizado de `PATCH /stock`) |

---

## Endpoints pendientes (no implementados aún)

| Endpoint sugerido | RF | Propósito |
|--------------------|----|-----------|
| `POST /api/auth/login` | RF-03 | Login con DNI + contraseña, devuelve JWT |
| `GET /api/reclamos/propios` | — | Listar reclamos del usuario autenticado |
| `GET /api/reclamos?estado=&zona=&prioridad=` | — | Filtros combinados de búsqueda |
| `GET /api/reclamos/{id}/paquete` | RF-11 | Vista consolidada: reporte del vecino + tipificación + prioridad + estado + observaciones técnicas |
| `POST /api/hojas-de-ruta/{id}/reclamos/batch` | RF-19 | Alta masiva de reclamos a una hoja de ruta |
| `GET /api/componentes` | RF-13 | Catálogo de componentes (lámpara, fotocontrol, balasto, cableado) |
| `GET /api/reportes/reclamos-por-zona` | RF-21 | Indicador para dashboard administrativo |
| `GET /api/reportes/tiempo-promedio-resolucion` | RF-21 | Indicador para dashboard administrativo |
| `GET /api/reportes/consumo-materiales` | RF-21 | Indicador para dashboard administrativo | 