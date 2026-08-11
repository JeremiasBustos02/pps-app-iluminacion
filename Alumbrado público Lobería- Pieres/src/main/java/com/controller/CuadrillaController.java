package com.controller;

import com.entity.Cuadrilla;
import com.service.CuadrillaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuadrillas")
public class CuadrillaController {

    @Autowired
    private CuadrillaService cuadrillaService;

    @GetMapping
    public ResponseEntity<List<Cuadrilla>> getAll() {
        return ResponseEntity.ok(cuadrillaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cuadrilla> getById(@PathVariable Long id) {
        return cuadrillaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cuadrilla> create(@RequestBody Cuadrilla cuadrilla) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuadrillaService.save(cuadrilla));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cuadrillaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cuadrilla> update(
            @PathVariable Long id,
            @RequestBody Cuadrilla cuadrilla) {
        return ResponseEntity.ok(cuadrillaService.update(id, cuadrilla));
    }
}