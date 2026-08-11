package com.controller;

import com.entity.TipoReclamo;
import com.service.TipoReclamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-reclamos")
public class TipoReclamoController {

    @Autowired
    private TipoReclamoService tipoReclamoService;

    @GetMapping
    public ResponseEntity<List<TipoReclamo>> getAll() {
        return ResponseEntity.ok(tipoReclamoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoReclamo> getById(@PathVariable Long id) {
        return tipoReclamoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoReclamo> create(@RequestBody TipoReclamo tipoReclamo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoReclamoService.save(tipoReclamo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tipoReclamoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}