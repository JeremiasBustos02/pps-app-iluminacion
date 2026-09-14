package com.service;

import com.dto.LuminariaDTO;
import com.dto.LuminariaHistorialDTO;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.entity.Zona;
import com.repository.LuminariaRepository;
import com.repository.ReclamoRepository;
import com.repository.ReparacionRepository;
import com.repository.ZonaRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LuminariaService {

    @Autowired
    private LuminariaRepository luminariaRepository;

    @Autowired
    private ZonaRepository zonaRepository;

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private ReparacionService reparacionService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<Luminaria> findAll() {
        return luminariaRepository.findByDeletedAtIsNull();
    }

    public Optional<Luminaria> findById(Long id) {
        return luminariaRepository.findById(id).filter(l -> l.getDeletedAt() == null);
    }

    public List<Luminaria> findByZona(Long zonaId) {
        return luminariaRepository.findByZonaIdAndDeletedAtIsNull(zonaId);
    }

    public Luminaria save(LuminariaDTO dto) {
        Luminaria luminaria = new Luminaria();
        mapDtoToEntity(dto, luminaria);
        return luminariaRepository.save(luminaria);
    }

    public Luminaria update(Long id, LuminariaDTO details) {
        Luminaria luminaria = findById(id)
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + id));
        mapDtoToEntity(details, luminaria);
        return luminariaRepository.save(luminaria);
    }

    public void delete(Long id) {
        Luminaria luminaria = findById(id)
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + id));
        luminaria.setDeletedAt(LocalDateTime.now());
        luminariaRepository.save(luminaria);
    }

    private void mapDtoToEntity(LuminariaDTO dto, Luminaria luminaria) {
        luminaria.setPotencia(dto.getPotencia());
        luminaria.setColumna(dto.getColumna());
        luminaria.setTipo(dto.getTipo());
        luminaria.setEstado(dto.getEstado());

        if (dto.getZonaId() != null) {
            Zona zona = zonaRepository.findById(dto.getZonaId())
                    .orElseThrow(() -> new RuntimeException("Zona no encontrada con ID: " + dto.getZonaId()));
            luminaria.setZona(zona);
        } else {
            luminaria.setZona(null);
        }

        if (dto.getLatitud() != null && dto.getLongitud() != null) {
            Point punto = geometryFactory.createPoint(new Coordinate(dto.getLongitud(), dto.getLatitud()));
            luminaria.setCoordenadas(punto);
        } else {
            luminaria.setCoordenadas(null);
        }
    }

    public List<Luminaria> filtrar(String estado, Long zonaId) {
        return luminariaRepository.filtrar(estado, zonaId);
    }

    // RF-14: historial de reclamos y observaciones de la cuadrilla sobre un mismo punto de luz,
    // usado como respaldo de auditoría ante reclamos recurrentes sobre la misma luminaria.
    @Transactional(readOnly = true)
    public LuminariaHistorialDTO getHistorial(Long luminariaId) {
        Luminaria luminaria = findById(luminariaId)
                .orElseThrow(() -> new RuntimeException("Luminaria no encontrada: " + luminariaId));

        LuminariaHistorialDTO dto = new LuminariaHistorialDTO();
        dto.setLuminariaId(luminaria.getId());
        dto.setTipo(luminaria.getTipo());
        dto.setEstado(luminaria.getEstado());
        if (luminaria.getZona() != null) {
            dto.setZona(luminaria.getZona().getNombre());
        }

        List<Reclamo> reclamos = reclamoRepository.findByLuminariaIdOrderByFechaDesc(luminariaId);
        List<LuminariaHistorialDTO.ReclamoHistorialEntry> entries = reclamos.stream()
                .map(reclamo -> {
                    LuminariaHistorialDTO.ReclamoHistorialEntry entry = new LuminariaHistorialDTO.ReclamoHistorialEntry();
                    entry.setId(reclamo.getId());
                    entry.setNumeroSeguimiento(reclamo.getNumeroSeguimiento());
                    entry.setEstado(reclamo.getEstado());
                    entry.setFecha(reclamo.getFecha());
                    if (reclamo.getTipoReclamo() != null) {
                        entry.setTipoReclamo(reclamo.getTipoReclamo().getNombre());
                    }

                    entry.setObservacionesCuadrilla(
                            reparacionRepository.findByReclamoIdOrderByFechaDesc(reclamo.getId()).stream()
                                    .map(reparacionService::toResumen)
                                    .toList());

                    return entry;
                })
                .toList();
        dto.setReclamos(entries);

        return dto;
    }
}