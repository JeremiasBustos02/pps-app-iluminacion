package com.controller;

import com.dto.LuminariaDTO;
import com.dto.LuminariaResponseDTO;
import com.entity.Luminaria;
import com.service.LuminariaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/luminarias")
public class LuminariaController {

    @Autowired
    private LuminariaService luminariaService;

    @GetMapping
    public ResponseEntity<List<LuminariaResponseDTO>> getAll() {
        List<LuminariaResponseDTO> list = luminariaService.findAll().stream()
                .map(LuminariaResponseDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LuminariaResponseDTO> getById(@PathVariable Long id) {
        return luminariaService.findById(id)
                .map(LuminariaResponseDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<LuminariaResponseDTO>> getByZona(@PathVariable Long zonaId) {
        List<LuminariaResponseDTO> list = luminariaService.findByZona(zonaId).stream()
                .map(luminaria -> new LuminariaResponseDTO(luminaria))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Luminaria> create(@RequestBody LuminariaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(luminariaService.save(dto));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Luminaria> update(@PathVariable Long id, @RequestBody LuminariaDTO dto) {
        return ResponseEntity.ok(luminariaService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        luminariaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}