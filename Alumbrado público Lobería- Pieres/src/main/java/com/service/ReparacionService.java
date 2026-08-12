package com.service;

import com.dto.MaterialConsumidoDTO;
import com.dto.ReparacionDTO;
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
    private MaterialRepository materialRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private ReparacionMaterialRepository reparacionMaterialRepository;

    @Autowired
    private ReparacionTecnicoRepository reparacionTecnicoRepository;

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

            reclamo.setEstado("RESUELTO");
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

        // 3. Registrar Materiales usados, descontar Stock y crear MovimientoStock
        if (dto.getMaterialesUsados() != null && !dto.getMaterialesUsados().isEmpty()) {
            for (MaterialConsumidoDTO matDto : dto.getMaterialesUsados()) {
                Material material = materialRepository.findById(matDto.getMaterialId())
                        .orElseThrow(() -> new RuntimeException("Material no encontrado: " + matDto.getMaterialId()));

                if (material.getCantidad() < matDto.getCantidad()) {
                    throw new RuntimeException("Stock insuficiente para: " + material.getNombre());
                }

                // Descontar stock
                material.setCantidad(material.getCantidad() - matDto.getCantidad());
                materialRepository.save(material);

                // Guardar relación ReparacionMaterial
                ReparacionMaterial repMat = new ReparacionMaterial();
                repMat.setReparacion(reparacionGuardada);
                repMat.setMaterial(material);
                repMat.setCantidad(matDto.getCantidad());
                reparacionMaterialRepository.save(repMat);

                // Guardar historial en MovimientoStock
                MovimientoStock movimiento = new MovimientoStock();
                movimiento.setReparacion(reparacionGuardada);
                movimiento.setMaterial(material);
                movimiento.setCantidad(matDto.getCantidad());
                movimiento.setTipo("SALIDA");
                movimiento.setFecha(LocalDateTime.now());
                movimientoStockRepository.save(movimiento);
            }
        }

        return reparacionGuardada;
    }

    public void delete(Long id) {
        reparacionRepository.deleteById(id);
    }
}