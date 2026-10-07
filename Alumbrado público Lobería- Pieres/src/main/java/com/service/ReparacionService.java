package com.service;

import com.enums.EstadoReclamo;
import com.dto.MaterialConsumidoDTO;
import com.dto.DisponibilidadMaterialesDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.dto.ReparacionResumenDTO;
import com.entity.*;
import com.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReparacionService {

    @Autowired
    private ReparacionRepository reparacionRepository;

    // @Lazy: ReclamoService también depende de este servicio (paquete de reclamo)
    @Autowired
    @Lazy
    private ReclamoService reclamoService;

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

    @Autowired
    private DisponibilidadMaterialService disponibilidadMaterialService;

    @Autowired
    private MaterialRepository materialRepository;

    public List<Reparacion> findAll() {
        return reparacionRepository.findAll();
    }

    public Optional<Reparacion> findById(Long id) {
        return reparacionRepository.findById(id);
    }

    @Transactional
    public ReparacionResponseDTO registrarReparacion(ReparacionDTO dto) {
        Reparacion reparacion = new Reparacion();
        reparacion.setObservacion(dto.getObservacion());
        reparacion.setFecha(LocalDateTime.now());

        // Diagnóstico (RF-13): componentes rotos informados por el técnico
        List<Componente> componentesRotos = new ArrayList<>();
        for (Long componenteId : dto.getComponentesDanadosIds()) {
            componentesRotos.add(componenteRepository.findByIdAndDeletedAtIsNull(componenteId)
                    .orElseThrow(() -> new IllegalArgumentException("Componente no encontrado: " + componenteId)));
        }

        // Materiales que la cuadrilla necesita usar en la reparación
        List<DisponibilidadMaterialService.MaterialRequerido> materialesUsados = new ArrayList<>();
        for (MaterialConsumidoDTO matDto : dto.getMaterialesUsados()) {
            if (matDto.getCantidad() == null || matDto.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
            Material material = materialRepository.findByIdAndDeletedAtIsNull(matDto.getMaterialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material no encontrado o eliminado: " + matDto.getMaterialId()));
            materialesUsados.add(new DisponibilidadMaterialService.MaterialRequerido(material, matDto.getCantidad()));
        }

        // RF-18: se verifica el stock de repuestos y materiales antes de descontar nada
        DisponibilidadMaterialesDTO disponibilidad =
                disponibilidadMaterialService.evaluar(componentesRotos, materialesUsados);

        // 1. Asociar Reclamo y resolverlo, o bloquearlo si falta algún repuesto
        if (dto.getReclamoId() != null) {
            // Pasa por updateEstado para validar la transición y dejar registro en el historial
            // (un reclamo en ESPERA_EDEA no puede resolverse sin antes recibir el alta de EDEA, RF-17)
            Reclamo reclamo = disponibilidad.isBloqueadoPorMaterial()
                    ? reclamoService.updateEstado(dto.getReclamoId(), EstadoReclamo.ESPERA_MATERIAL,
                            "Bloqueado por falta de material: " + repuestosFaltantes(disponibilidad))
                    : reclamoService.updateEstado(dto.getReclamoId(), EstadoReclamo.RESUELTO,
                            "Resuelto al registrar la reparación");

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

        // 3. Registrar Materiales delegando en ReparacionMaterialService.
        // Los que no tienen stock no se descuentan: quedan como faltantes y el reclamo en ESPERA_MATERIAL (RF-18)
        List<Long> faltantes = disponibilidad.getRepuestos().stream()
                .filter(r -> !r.isDisponible())
                .map(DisponibilidadMaterialesDTO.RepuestoInfo::getMaterialId)
                .toList();
        for (DisponibilidadMaterialService.MaterialRequerido usado : materialesUsados) {
            if (faltantes.contains(usado.material().getId())) {
                continue;
            }
            ReparacionMaterial repMat = new ReparacionMaterial();
            repMat.setReparacion(reparacionGuardada);
            repMat.setMaterial(usado.material());
            repMat.setCantidad(usado.cantidad());

            reparacionMaterialService.addMaterial(repMat);
        }

        // 4. Registrar Componentes dañados
        for (Componente comp : componentesRotos) {
            ReparacionComponente repComp = new ReparacionComponente();
            repComp.setReparacion(reparacionGuardada);
            repComp.setComponente(comp);
            reparacionComponenteRepository.save(repComp);
        }

        ReparacionResponseDTO response = new ReparacionResponseDTO(reparacionGuardada);
        response.setDisponibilidadMateriales(disponibilidad);
        return response;
    }

    private String repuestosFaltantes(DisponibilidadMaterialesDTO disponibilidad) {
        String faltantes = disponibilidad.getRepuestos().stream()
                .filter(r -> !r.isDisponible())
                .map(DisponibilidadMaterialesDTO.RepuestoInfo::getMaterial)
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
        // la observación del historial admite hasta 255 caracteres
        return faltantes.length() > 200 ? faltantes.substring(0, 200) + "..." : faltantes;
    }

    // RF-18: disponibilidad actual de los repuestos del último diagnóstico
    @Transactional(readOnly = true)
    public DisponibilidadMaterialesDTO disponibilidadDelDiagnostico(Reparacion reparacion) {
        List<Componente> componentes = reparacion.getComponentesAveriados().stream()
                .map(ReparacionComponente::getComponente)
                .toList();
        return disponibilidadMaterialService.evaluar(componentes);
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