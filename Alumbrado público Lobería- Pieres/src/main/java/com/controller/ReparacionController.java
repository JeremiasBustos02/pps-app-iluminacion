package com.controller;

import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.entity.Reparacion;
import com.service.ReparacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reparaciones")
public class ReparacionController {

    @Autowired
    private ReparacionService reparacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<List<ReparacionResponseDTO>> getAll() {
        List<ReparacionResponseDTO> list = reparacionService.findAll().stream()
                .map(ReparacionResponseDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ReparacionResponseDTO> getById(@PathVariable Long id) {
        return reparacionService.findById(id)
                .map(r -> ResponseEntity.ok(new ReparacionResponseDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ReparacionResponseDTO> create(@RequestBody ReparacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reparacionService.registrarReparacion(dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reparacionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}