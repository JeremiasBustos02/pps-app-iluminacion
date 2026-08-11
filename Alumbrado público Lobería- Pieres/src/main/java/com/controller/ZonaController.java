package com.controller;

import com.entity.Zona;
import com.service.ZonaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    @Autowired
    private ZonaService zonaService;

    @GetMapping
    public ResponseEntity<List<Zona>> getAll() {
        return ResponseEntity.ok(zonaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Zona> getById(@PathVariable Long id) {
        return zonaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Zona> create(@RequestBody Zona zona) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.save(zona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Zona> update(@PathVariable Long id, @RequestBody Zona zona) {
        return ResponseEntity.ok(zonaService.update(id, zona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zonaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}