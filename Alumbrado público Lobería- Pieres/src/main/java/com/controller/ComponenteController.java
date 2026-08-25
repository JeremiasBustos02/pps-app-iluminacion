package com.controller;

import com.entity.Componente;
import com.service.ComponenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/componentes")
public class ComponenteController {

    @Autowired
    private ComponenteService componenteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<Componente>> getAll() {
        return ResponseEntity.ok(componenteService.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Componente> create(@RequestBody Componente componente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(componenteService.save(componente));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        componenteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}