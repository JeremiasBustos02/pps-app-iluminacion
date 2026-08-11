package com.controller;

import com.entity.Reclamo;
import com.service.ReclamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamos")
public class ReclamoController {

    @Autowired
    private ReclamoService reclamoService;

    @GetMapping
    public ResponseEntity<List<Reclamo>> getAll() {
        return ResponseEntity.ok(reclamoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reclamo> getById(@PathVariable Long id) {
        return reclamoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seguimiento/{numeroSeguimiento}")
    public ResponseEntity<Reclamo> getBySeguimiento(@PathVariable String numeroSeguimiento) {
        return reclamoService.findByNumeroSeguimiento(numeroSeguimiento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reclamo> create(@RequestBody Reclamo reclamo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reclamoService.save(reclamo));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Reclamo> updateEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(reclamoService.updateEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}