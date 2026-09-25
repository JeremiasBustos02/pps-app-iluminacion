package com.service;

import com.dto.DisponibilidadMaterialesDTO;
import com.dto.ReclamoDTO;
import com.entity.*;
import com.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.enums.EstadoReclamo;
import org.springframework.transaction.annotation.Transactional;
import com.dto.ReclamoPaqueteDTO;
import com.entity.ReclamoHistorial;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
public class ReclamoService {

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private LuminariaRepository luminariaRepository;

    @Autowired
    private TipoReclamoRepository tipoReclamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReclamoHistorialRepository reclamoHistorialRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private ReparacionService reparacionService;

    @Autowired
    private TiempoEstimadoService tiempoEstimadoService;

    public List<Reclamo> findAll() {
        return reclamoRepository.findAll();
    }

    public Optional<Reclamo> findById(Long id) {
        return reclamoRepository.findById(id);
    }

    public Optional<Reclamo> findByNumeroSeguimiento(String numeroSeguimiento) {
        return reclamoRepository.findByNumeroSeguimiento(numeroSeguimiento);
    }

    public List<Reclamo> findByUsuario(Long usuarioId) {
        return reclamoRepository.findByUsuarioId(usuarioId);
    }

    public Reclamo saveFromDTO(ReclamoDTO dto) {
        // RF-07: el reclamo se genera seleccionando un punto en el mapa (luminaria)
        // y tipificando el problema a partir del catálogo predefinido; ambos son obligatorios.
        if (dto.getLuminariaId() == null) {
            throw new RuntimeException("Debe seleccionar una luminaria en el mapa");
        }
        if (dto.getTipoReclamoId() == null) {
            throw new RuntimeException("Debe seleccionar un tipo de problema");
        }

        Reclamo reclamo = new Reclamo();

        // 1. Asignar Luminaria (punto seleccionado en el mapa)
        Luminaria luminaria = luminariaRepository.findById(dto.getLuminariaId())
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + dto.getLuminariaId()));
        reclamo.setLuminaria(luminaria);

        // 2. Asignar TipoReclamo (tipificación del catálogo predefinido)
        TipoReclamo tipoReclamo = tipoReclamoRepository.findById(dto.getTipoReclamoId())
                .orElseThrow(() -> new RuntimeException("Tipo de reclamo no encontrado: " + dto.getTipoReclamoId()));
        reclamo.setTipoReclamo(tipoReclamo);

        // 3. Asignar Usuario
        if (dto.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + dto.getUsuarioId()));
            reclamo.setUsuario(usuario);
        }

        // RF-08: número de seguimiento único, formato REC-YYYY-NNNNN.
        // El correlativo sale de la secuencia de la base (reclamo_numero_seq), no del reloj:
        // así se garantiza unicidad real incluso con altas concurrentes.
        String anioActual = String.valueOf(Year.now().getValue());
        Long correlativo = reclamoRepository.siguienteNumeroSeguimiento();
        reclamo.setNumeroSeguimiento(String.format("REC-%s-%05d", anioActual, correlativo));

        if (dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            String observacion = dto.getObservacion().trim();
            if (observacion.length() > 500) {
                throw new RuntimeException("La observación no puede superar los 500 caracteres");
            }
            reclamo.setObservacion(observacion);
        }

        // Valores por defecto
        reclamo.setEstado(EstadoReclamo.PENDIENTE);
        reclamo.setFecha(LocalDateTime.now());

        // RF-10: tiempo estimado según prioridad, zona y carga de las cuadrillas
        Integer horas = tiempoEstimadoService.calcular(tipoReclamo, luminaria);
        reclamo.setTiempoEstimado(horas);
        if (horas != null) {
            reclamo.setFechaLimite(reclamo.getFecha().plusHours(horas));
        }

        return reclamoRepository.save(reclamo);
    }

    @Transactional
    public Reclamo updateEstado(Long id, EstadoReclamo nuevoEstado, String observacion) {
        Reclamo reclamo = findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));

        EstadoReclamo estadoAnterior = reclamo.getEstado();

        if (estadoAnterior == nuevoEstado) {
            return reclamo;
        }

        if (!estadoAnterior.puedeTransicionarA(nuevoEstado)) {
            throw new RuntimeException(
                    "No se puede pasar de " + estadoAnterior + " a " + nuevoEstado);
        }

        // RF-17: mientras el reclamo espera el alta de EDEA el SLA municipal queda pausado
        if (nuevoEstado == EstadoReclamo.ESPERA_EDEA) {
            pausarSla(reclamo, LocalDateTime.now());
        } else if (estadoAnterior == EstadoReclamo.ESPERA_EDEA) {
            reanudarSla(reclamo, LocalDateTime.now());
        }

        // RF-18: la falta de repuesto no pausa el SLA (es material municipal), pero corre el plazo
        // informado al vecino por el tiempo de reposición
        if (nuevoEstado == EstadoReclamo.ESPERA_MATERIAL) {
            extenderPlazo(reclamo, TiempoEstimadoService.HORAS_REPOSICION_MATERIAL * 60L);
        }

        // 1. Actualizar el estado en el reclamo
        reclamo.setEstado(nuevoEstado);
        Reclamo reclamoGuardado = reclamoRepository.save(reclamo);

        // 2. Crear y guardar el registro en el historial
        ReclamoHistorial historial = new ReclamoHistorial();
        historial.setReclamo(reclamoGuardado);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setObservacion(observacion);

        reclamoHistorialRepository.save(historial);

        return reclamoGuardado;
    }

    void pausarSla(Reclamo reclamo, LocalDateTime ahora) {
        if (reclamo.getSlaPausadoDesde() == null) {
            reclamo.setSlaPausadoDesde(ahora);
        }
    }

    // RF-17 / RF-10: al recibir el alta de EDEA se corre el plazo por el tiempo pausado
    // y se actualiza el tiempo estimado informado al vecino
    void reanudarSla(Reclamo reclamo, LocalDateTime ahora) {
        LocalDateTime desde = reclamo.getSlaPausadoDesde();
        if (desde == null) {
            return;
        }
        long minutos = Math.max(0, Duration.between(desde, ahora).toMinutes());
        reclamo.setMinutosPausa(reclamo.getMinutosPausa() + (int) minutos);
        reclamo.setSlaPausadoDesde(null);

        extenderPlazo(reclamo, minutos);
    }

    // Corre la fecha límite y actualiza el tiempo estimado informado al vecino (RF-10)
    void extenderPlazo(Reclamo reclamo, long minutos) {
        if (reclamo.getFechaLimite() == null) {
            return;
        }
        reclamo.setFechaLimite(reclamo.getFechaLimite().plusMinutes(minutos));
        if (reclamo.getFecha() != null) {
            long totalMinutos = Duration.between(reclamo.getFecha(), reclamo.getFechaLimite()).toMinutes();
            reclamo.setTiempoEstimado((int) Math.ceil(totalMinutos / 60.0));
        }
    }

    public List<Reclamo> filtrar(EstadoReclamo estado, Long zonaId, Long tipoReclamoId) {
        return reclamoRepository.filtrar(estado, zonaId, tipoReclamoId);
    }

    @Transactional(readOnly = true)
    public ReclamoPaqueteDTO getPaquete(Long id) {
        // 1. Buscar el reclamo
        Reclamo reclamo = reclamoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + id));

        // 2. Buscar el historial
        List<ReclamoHistorial> historial = reclamoHistorialRepository
                .findByReclamoIdOrderByFechaCambioDesc(id);

        // 3. Crear el DTO y setear datos del reclamo
        ReclamoPaqueteDTO dto = new ReclamoPaqueteDTO();
        dto.setId(reclamo.getId());
        dto.setNumeroSeguimiento(reclamo.getNumeroSeguimiento());
        dto.setEstado(reclamo.getEstado());
        dto.setFecha(reclamo.getFecha());
        dto.setTiempoEstimado(reclamo.getTiempoEstimado());
        dto.setSlaPausado(reclamo.getSlaPausadoDesde() != null);
        dto.setFechaEstimadaResolucion(dto.isSlaPausado() ? null : reclamo.getFechaLimite());
        dto.setMinutosPausa(reclamo.getMinutosPausa());

        // 4. Bloque tipo de reclamo
        if (reclamo.getTipoReclamo() != null) {
            ReclamoPaqueteDTO.TipoReclamoInfo tr = new ReclamoPaqueteDTO.TipoReclamoInfo();
            tr.setId(reclamo.getTipoReclamo().getId());
            tr.setNombre(reclamo.getTipoReclamo().getNombre());
            tr.setPrioridad(reclamo.getTipoReclamo().getPrioridad());
            dto.setTipoReclamo(tr);
        }

        // 5. Bloque vecino
        if (reclamo.getUsuario() != null) {
            ReclamoPaqueteDTO.VecinoInfo v = new ReclamoPaqueteDTO.VecinoInfo();
            v.setId(reclamo.getUsuario().getId());
            v.setNombre(reclamo.getUsuario().getNombre());
            v.setDni(reclamo.getUsuario().getDni());
            v.setEmail(reclamo.getUsuario().getEmail());
            dto.setVecino(v);
        }

        // 6. Bloque luminaria
        if (reclamo.getLuminaria() != null) {
            ReclamoPaqueteDTO.LuminariaInfo l = new ReclamoPaqueteDTO.LuminariaInfo();
            l.setId(reclamo.getLuminaria().getId());
            l.setTipo(reclamo.getLuminaria().getTipo());
            l.setEstado(reclamo.getLuminaria().getEstado());
            if (reclamo.getLuminaria().getZona() != null) {
                l.setZona(reclamo.getLuminaria().getZona().getNombre());
            }
            dto.setLuminaria(l);
        }

        // 7. Bloque historial
        List<ReclamoPaqueteDTO.HistorialInfo> historialDTO = historial.stream()
                .map(h -> {
                    ReclamoPaqueteDTO.HistorialInfo hi = new ReclamoPaqueteDTO.HistorialInfo();
                    hi.setEstadoAnterior(h.getEstadoAnterior());
                    hi.setEstadoNuevo(h.getEstadoNuevo());
                    hi.setObservacion(h.getObservacion());
                    hi.setFechaCambio(h.getFechaCambio());
                    return hi;
                })
                .toList();
        dto.setHistorial(historialDTO);

        // 8. Bloque observaciones de la cuadrilla (RF-14)
        List<Reparacion> reparaciones = reparacionRepository.findByReclamoIdOrderByFechaDesc(id);
        dto.setObservacionesCuadrilla(reparaciones.stream().map(reparacionService::toResumen).toList());

        // 9. RF-18: disponibilidad actual de los repuestos del último diagnóstico
        if (!reparaciones.isEmpty()) {
            DisponibilidadMaterialesDTO disponibilidad =
                    reparacionService.disponibilidadDelDiagnostico(reparaciones.get(0));
            disponibilidad.setBloqueadoPorMaterial(reclamo.getEstado() == EstadoReclamo.ESPERA_MATERIAL);
            dto.setDisponibilidadMateriales(disponibilidad);
        }

        return dto;
    }

    public void delete(Long id) {
        reclamoRepository.deleteById(id);
    }
}