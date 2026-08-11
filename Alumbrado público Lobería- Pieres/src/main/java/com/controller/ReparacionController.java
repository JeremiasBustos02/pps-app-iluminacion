package com.controller;

import com.entity.Reparacion;
import com.service.ReparacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reparaciones")
public class ReparacionController {

    @Autowired
    private ReparacionService reparacionService;

    @GetMapping
    public ResponseEntity<List<Reparacion>> getAll() {
        return ResponseEntity.ok(reparacionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reparacion> getById(@PathVariable Long id) {
        return reparacionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Reparacion> create(@RequestBody Reparacion reparacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reparacionService.save(reparacion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reparacionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}