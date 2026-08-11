package com.service;

import com.dto.LuminariaDTO;
import com.entity.Luminaria;
import com.entity.Zona;
import com.repository.LuminariaRepository;
import com.repository.ZonaRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LuminariaService {

    @Autowired
    private LuminariaRepository luminariaRepository;

    @Autowired
    private ZonaRepository zonaRepository;

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
}