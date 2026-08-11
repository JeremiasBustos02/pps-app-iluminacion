package com.service;

import com.entity.MovimientoStock;
import com.repository.MovimientoStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovimientoStockService {

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    public List<MovimientoStock> findAll() {
        return movimientoStockRepository.findAll();
    }

    public List<MovimientoStock> findByMaterial(Long materialId) {
        return movimientoStockRepository.findByMaterialId(materialId);
    }

    public MovimientoStock registrarMovimiento(MovimientoStock movimiento) {
        return movimientoStockRepository.save(movimiento);
    }
}