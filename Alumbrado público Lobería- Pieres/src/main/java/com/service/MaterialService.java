package com.service;

import com.entity.Material;
import com.entity.MovimientoStock;
import com.enums.TipoMovimiento;
import com.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private MovimientoStockService movimientoStockService;

    public List<Material> findAll() {
        return materialRepository.findByDeletedAtIsNull();
    }

    public Optional<Material> findById(Long id) {
        return materialRepository.findById(id).filter(m -> m.getDeletedAt() == null);
    }

    @Transactional
    public Material save(Material material) {
        return materialRepository.save(material);
    }

    @Transactional
    public Material updateStock(Long id, Integer cantidadMovimiento, TipoMovimiento tipo) {
        MovimientoStock movimiento = MovimientoStock.builder()
                .material(Material.builder().id(id).build())
                .cantidad(cantidadMovimiento)
                .tipo(tipo)
                .build();

        MovimientoStock guardado = movimientoStockService.registrarMovimiento(movimiento);
        return guardado.getMaterial();
    }

    // RF-21: actualizar el precio unitario (no modifica los movimientos ya registrados)
    @Transactional
    public Material updatePrecio(Long id, BigDecimal precioUnitario) {
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
        Material material = findById(id).orElseThrow(() -> new RuntimeException("Material no encontrado: " + id));
        material.setPrecioUnitario(precioUnitario);
        return materialRepository.save(material);
    }

    @Transactional
    public void delete(Long id) {
        Material material = findById(id).orElseThrow(() -> new RuntimeException("Material no encontrado: " + id));
        material.setDeletedAt(LocalDateTime.now());
        materialRepository.save(material);
    }
}