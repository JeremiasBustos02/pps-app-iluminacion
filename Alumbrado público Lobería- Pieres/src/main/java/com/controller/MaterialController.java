package com.controller;

import com.entity.Material;
import com.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiales")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<Material>> getAll() {
        return ResponseEntity.ok(materialService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<Material> getById(@PathVariable Long id) {
        return materialService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Material> create(@RequestBody Material material) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.save(material));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Material> updateStock(@PathVariable Long id, @RequestParam Integer cantidad) {
        return ResponseEntity.ok(materialService.updateStock(id, cantidad));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}