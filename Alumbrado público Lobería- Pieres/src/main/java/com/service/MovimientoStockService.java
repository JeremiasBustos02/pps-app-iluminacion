package com.service;

import com.entity.Material;
import com.entity.MovimientoStock;
import com.repository.MaterialRepository;
import com.repository.MovimientoStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoStockService {

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private MaterialRepository materialRepository;

    public List<MovimientoStock> findAll() {
        return movimientoStockRepository.findAll();
    }

    public List<MovimientoStock> findByMaterial(Long materialId) {
        return movimientoStockRepository.findByMaterialId(materialId);
    }

    @Transactional
    public MovimientoStock registrarMovimiento(MovimientoStock movimiento) {
        // 1. Validaciones de carga básica
        if (movimiento.getMaterial() == null || movimiento.getMaterial().getId() == null) {
            throw new IllegalArgumentException("Debe especificar un material válido");
        }
        if (movimiento.getCantidad() == null || movimiento.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        if (movimiento.getTipo() == null || (!movimiento.getTipo().equalsIgnoreCase("ingreso") && !movimiento.getTipo().equalsIgnoreCase("egreso"))) {
            throw new IllegalArgumentException("Tipo de movimiento inválido. Debe ser 'ingreso' o 'egreso'");
        }

        // 2. Buscar material activo
        Material mat = materialRepository.findByIdAndDeletedAtIsNull(movimiento.getMaterial().getId())
                .orElseThrow(() -> new IllegalArgumentException("Material no encontrado o eliminado: " + movimiento.getMaterial().getId()));

        int stockActual = mat.getCantidad() != null ? mat.getCantidad() : 0;

        // 3. Aplicar lógica según el tipo
        if (movimiento.getTipo().equalsIgnoreCase("egreso")) {
            if (stockActual < movimiento.getCantidad()) {
                throw new IllegalArgumentException("Cantidad insuficiente en stock. Stock disponible: " + stockActual);
            }
            mat.setCantidad(stockActual - movimiento.getCantidad());
        } else {
            mat.setCantidad(stockActual + movimiento.getCantidad());
        }

        // 4. Asignar fecha si no viene seteada
        if (movimiento.getFecha() == null) {
            movimiento.setFecha(LocalDateTime.now());
        }

        // 5. Vincular y persistir ambas entidades
        movimiento.setMaterial(mat);
        materialRepository.save(mat);

        return movimientoStockRepository.save(movimiento);
    }
}