package com.controller;

import com.entity.MovimientoStock;
import com.service.MovimientoStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos-stock")
public class MovimientoStockController {

    @Autowired
    private MovimientoStockService movimientoStockService;

    @GetMapping
    public ResponseEntity<List<MovimientoStock>> getAll() {
        return ResponseEntity.ok(movimientoStockService.findAll());
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<MovimientoStock>> getByMaterial(@PathVariable Long materialId) {
        return ResponseEntity.ok(movimientoStockService.findByMaterial(materialId));
    }

    @PostMapping
    public ResponseEntity<MovimientoStock> create(@RequestBody MovimientoStock movimiento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoStockService.registrarMovimiento(movimiento));
    }
}