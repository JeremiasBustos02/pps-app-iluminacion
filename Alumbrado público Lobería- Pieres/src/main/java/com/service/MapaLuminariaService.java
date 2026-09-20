package com.service;

import com.dto.LuminariaDetalleMapaDTO;
import com.dto.LuminariaMapaDTO;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.enums.ColorLuminaria;
import com.enums.EstadoReclamo;
import com.repository.LuminariaRepository;
import com.repository.ReclamoActivoView;
import com.repository.ReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RF-05: mapa de luminarias con semáforo de estado
@Service
public class MapaLuminariaService {

    static final int PRIORIDAD_ALTA = 3;
    static final String ESTADO_OPERATIVO = "Funciona";

    private static final List<EstadoReclamo> ESTADOS_ACTIVOS = Arrays.stream(EstadoReclamo.values())
            .filter(EstadoReclamo::esActivo)
            .toList();

    @Autowired
    private LuminariaRepository luminariaRepository;

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Transactional(readOnly = true)
    public List<LuminariaMapaDTO> getMapa(Long zonaId) {
        Map<Long, List<ReclamoActivoView>> activosPorLuminaria = new HashMap<>();
        for (ReclamoActivoView activo : reclamoRepository.findActivos(ESTADOS_ACTIVOS)) {
            activosPorLuminaria.computeIfAbsent(activo.getLuminariaId(), k -> new ArrayList<>()).add(activo);
        }

        return luminariaRepository.filtrar(null, zonaId).stream()
                .filter(l -> l.getCoordenadas() != null)
                .map(l -> toMapaDTO(l, activosPorLuminaria.getOrDefault(l.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LuminariaDetalleMapaDTO getDetalle(Long luminariaId) {
        Luminaria luminaria = luminariaRepository.findById(luminariaId)
                .filter(l -> l.getDeletedAt() == null)
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + luminariaId));

        List<Reclamo> activos = reclamoRepository
                .findByLuminariaIdAndEstadoInOrderByFechaDesc(luminariaId, ESTADOS_ACTIVOS);

        LuminariaDetalleMapaDTO dto = new LuminariaDetalleMapaDTO();
        dto.setId(luminaria.getId());
        dto.setColor(calcularColor(
                activos.stream().map(r -> new Activo(r.getEstado(), prioridadDe(r))).toList(),
                luminaria.getEstado()));
        dto.setTecnologia(luminaria.getTipo());
        dto.setPotencia(luminaria.getPotencia());
        dto.setColumna(luminaria.getColumna());
        dto.setEstado(luminaria.getEstado());
        if (luminaria.getZona() != null) {
            dto.setZonaNombre(luminaria.getZona().getNombre());
            dto.setMarcaGris(!luminaria.getZona().isAreaUrbana());
        }
        if (luminaria.getCoordenadas() != null) {
            dto.setLatitud(luminaria.getCoordenadas().getY());
            dto.setLongitud(luminaria.getCoordenadas().getX());
        }

        dto.setObservacionesVecino(activos.stream().map(r -> {
            LuminariaDetalleMapaDTO.ObservacionVecino o = new LuminariaDetalleMapaDTO.ObservacionVecino();
            o.setReclamoId(r.getId());
            o.setNumeroSeguimiento(r.getNumeroSeguimiento());
            o.setEstado(r.getEstado());
            o.setFecha(r.getFecha());
            o.setObservacion(r.getObservacion());
            if (r.getTipoReclamo() != null) {
                o.setTipoReclamo(r.getTipoReclamo().getNombre());
                o.setPrioridad(r.getTipoReclamo().getPrioridad());
            }
            return o;
        }).toList());

        return dto;
    }

    private LuminariaMapaDTO toMapaDTO(Luminaria luminaria, List<ReclamoActivoView> activos) {
        LuminariaMapaDTO dto = new LuminariaMapaDTO();
        dto.setId(luminaria.getId());
        dto.setLatitud(luminaria.getCoordenadas().getY());
        dto.setLongitud(luminaria.getCoordenadas().getX());
        dto.setColor(calcularColor(
                activos.stream().map(a -> new Activo(a.getEstado(), a.getPrioridad())).toList(),
                luminaria.getEstado()));
        if (luminaria.getZona() != null) {
            dto.setZonaId(luminaria.getZona().getId());
            dto.setZonaNombre(luminaria.getZona().getNombre());
            dto.setMarcaGris(!luminaria.getZona().isAreaUrbana());
        }
        return dto;
    }

    private Integer prioridadDe(Reclamo reclamo) {
        return reclamo.getTipoReclamo() != null ? reclamo.getTipoReclamo().getPrioridad() : null;
    }

    record Activo(EstadoReclamo estado, Integer prioridad) {}

    // Rojo: reclamo activo de prioridad alta, o sin servicio confirmado (esperando conexión de EDEA
    // o luminaria marcada fuera de servicio). Amarillo: cualquier otro reclamo activo. Verde: sin reclamos activos.
    static ColorLuminaria calcularColor(Collection<Activo> activos, String estadoLuminaria) {
        boolean sinServicio = estadoLuminaria != null && !estadoLuminaria.isBlank()
                && !ESTADO_OPERATIVO.equalsIgnoreCase(estadoLuminaria.trim());
        boolean hayActivos = false;
        for (Activo a : activos) {
            if (a.estado() == null || !a.estado().esActivo()) {
                continue;
            }
            hayActivos = true;
            if ((a.prioridad() != null && a.prioridad() >= PRIORIDAD_ALTA)
                    || a.estado() == EstadoReclamo.ESPERA_EDEA) {
                return ColorLuminaria.ROJO;
            }
        }
        if (sinServicio) {
            return ColorLuminaria.ROJO;
        }
        return hayActivos ? ColorLuminaria.AMARILLO : ColorLuminaria.VERDE;
    }
}
