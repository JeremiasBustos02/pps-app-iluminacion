package com.service;

import com.enums.EstadoReclamo;
import com.dto.MaterialConsumidoDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResumenDTO;
import com.entity.*;
import com.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReparacionService {

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReparacionTecnicoRepository reparacionTecnicoRepository;

    @Autowired
    private ReparacionMaterialService reparacionMaterialService;

    @Autowired
    private ComponenteRepository componenteRepository;

    @Autowired
    private ReparacionComponenteRepository reparacionComponenteRepository;

    public List<Reparacion> findAll() {
        return reparacionRepository.findAll();
    }

    public Optional<Reparacion> findById(Long id) {
        return reparacionRepository.findById(id);
    }

    @Transactional
    public Reparacion registrarReparacion(ReparacionDTO dto) {
        Reparacion reparacion = new Reparacion();
        reparacion.setObservacion(dto.getObservacion());
        reparacion.setFecha(LocalDateTime.now());

        // 1. Asociar Reclamo y resolverlo
        if (dto.getReclamoId() != null) {
            Reclamo reclamo = reclamoRepository.findById(dto.getReclamoId())
                    .orElseThrow(() -> new RuntimeException("Reclamo no encontrado: " + dto.getReclamoId()));

            reclamo.setEstado(EstadoReclamo.RESUELTO);
            reclamoRepository.save(reclamo);

            reparacion.setReclamo(reclamo);
        }

        Reparacion reparacionGuardada = reparacionRepository.save(reparacion);

        // 2. Registrar Técnicos / Cuadrilla asociada
        if (dto.getTecnicosIds() != null && !dto.getTecnicosIds().isEmpty()) {
            for (Long tecnicoId : dto.getTecnicosIds()) {
                Usuario tecnico = usuarioRepository.findById(tecnicoId)
                        .orElseThrow(() -> new RuntimeException("Técnico no encontrado: " + tecnicoId));

                ReparacionTecnico repTec = new ReparacionTecnico();
                repTec.setReparacion(reparacionGuardada);
                repTec.setUsuario(tecnico);
                reparacionTecnicoRepository.save(repTec);
            }
        }

        // 3. Registrar Materiales delegando en ReparacionMaterialService
        if (dto.getMaterialesUsados() != null && !dto.getMaterialesUsados().isEmpty()) {
            for (MaterialConsumidoDTO matDto : dto.getMaterialesUsados()) {
                Material materialRef = new Material();
                materialRef.setId(matDto.getMaterialId());

                ReparacionMaterial repMat = new ReparacionMaterial();
                repMat.setReparacion(reparacionGuardada);
                repMat.setMaterial(materialRef);
                repMat.setCantidad(matDto.getCantidad());

                reparacionMaterialService.addMaterial(repMat);
            }
        }

        // 4. Registrar Componentes dañados
        if (dto.getComponentesDanadosIds() != null && !dto.getComponentesDanadosIds().isEmpty()) {
            for (Long componenteId : dto.getComponentesDanadosIds()) {
                Componente comp = componenteRepository.findByIdAndDeletedAtIsNull(componenteId)
                        .orElseThrow(() -> new IllegalArgumentException("Componente no encontrado: " + componenteId));

                ReparacionComponente repComp = new ReparacionComponente();
                repComp.setReparacion(reparacionGuardada);
                repComp.setComponente(comp);
                reparacionComponenteRepository.save(repComp);
            }
        }

        return reparacionGuardada;
    }

    public void delete(Long id) {
        reparacionRepository.deleteById(id);
    }

    // RF-14: resumen de una reparación (observación + técnicos + componentes averiados),
    // reutilizado tanto en el paquete de reclamo (RF-11) como en el historial por luminaria.
    @Transactional(readOnly = true)
    public ReparacionResumenDTO toResumen(Reparacion reparacion) {
        ReparacionResumenDTO dto = new ReparacionResumenDTO();
        dto.setId(reparacion.getId());
        dto.setObservacion(reparacion.getObservacion());
        dto.setFecha(reparacion.getFecha());

        List<String> tecnicos = reparacionTecnicoRepository.findByReparacionId(reparacion.getId()).stream()
                .map(rt -> rt.getUsuario().getNombre())
                .toList();
        dto.setTecnicos(tecnicos);

        List<String> componentes = reparacion.getComponentesAveriados().stream()
                .map(rc -> rc.getComponente().getNombre() + " (" + rc.getEstadoComponente() + ")")
                .toList();
        dto.setComponentesAveriados(componentes);

        return dto;
    }
}