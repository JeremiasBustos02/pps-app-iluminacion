package com.service;

import com.entity.MovimientoStock;
import com.entity.ReparacionMaterial;
import com.repository.ReparacionMaterialRepository;
import com.repository.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReparacionMaterialService {

    @Autowired
    private ReparacionMaterialRepository reparacionMaterialRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private MovimientoStockService movimientoStockService;

    public List<ReparacionMaterial> findByReparacion(Long reparacionId) {
        return reparacionMaterialRepository.findByReparacionId(reparacionId);
    }

    @Transactional
    public ReparacionMaterial addMaterial(ReparacionMaterial reparacionMaterial) {
        // 1. Validaciones básicas
        if (reparacionMaterial.getMaterial() == null || reparacionMaterial.getMaterial().getId() == null) {
            throw new IllegalArgumentException("Debe especificar un material válido");
        }
        if (reparacionMaterial.getReparacion() == null || reparacionMaterial.getReparacion().getId() == null) {
            throw new IllegalArgumentException("Debe especificar una reparación válida");
        }
        if (reparacionMaterial.getCantidad() == null || reparacionMaterial.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        // 2. Verificar existencia de la reparación
        if (!reparacionRepository.existsById(reparacionMaterial.getReparacion().getId())) {
            throw new IllegalArgumentException("Reparación no encontrada: " + reparacionMaterial.getReparacion().getId());
        }

        // 3. Registrar el egreso
        MovimientoStock movStock = MovimientoStock.builder()
                .material(reparacionMaterial.getMaterial())
                .reparacion(reparacionMaterial.getReparacion())
                .tipo("egreso")
                .cantidad(reparacionMaterial.getCantidad())
                .fecha(LocalDateTime.now())
                .build();

        movimientoStockService.registrarMovimiento(movStock);

        return reparacionMaterialRepository.save(reparacionMaterial);
    }
}