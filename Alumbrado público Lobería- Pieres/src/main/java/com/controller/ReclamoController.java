package com.controller;

import com.dto.ReclamoDTO;
import com.dto.ReclamoResponseDTO;
import com.entity.Reclamo;
import com.enums.EstadoReclamo;
import com.service.ReclamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamos")
public class ReclamoController {

    @Autowired
    private ReclamoService reclamoService;

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @GetMapping
    public ResponseEntity<List<ReclamoResponseDTO>> getAll(
            @RequestParam(required = false) EstadoReclamo estado,
            @RequestParam(required = false) Long zonaId,
            @RequestParam(required = false) Long tipoReclamoId) {
        List<ReclamoResponseDTO> list = reclamoService.filtrar(estado, zonaId, tipoReclamoId).stream()
                .map(ReclamoResponseDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReclamoResponseDTO> getById(@PathVariable Long id) {
        return reclamoService.findById(id)
                .map(r -> ResponseEntity.ok(new ReclamoResponseDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/seguimiento/{numeroSeguimiento}")
    public ResponseEntity<ReclamoResponseDTO> getBySeguimiento(@PathVariable String numeroSeguimiento) {
        return reclamoService.findByNumeroSeguimiento(numeroSeguimiento)
                .map(r -> ResponseEntity.ok(new ReclamoResponseDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasAnyRole('VECINO', 'ADMINISTRADOR')")
    public ResponseEntity<List<ReclamoResponseDTO>> getByUsuario(@PathVariable Long usuarioId) {
        List<ReclamoResponseDTO> list = reclamoService.findByUsuario(usuarioId).stream()
                .map(ReclamoResponseDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VECINO', 'ADMINISTRADOR')")
    public ResponseEntity<ReclamoResponseDTO> create(@RequestBody ReclamoDTO dto) {
        Reclamo nuevoReclamo = reclamoService.saveFromDTO(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ReclamoResponseDTO(nuevoReclamo));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ReclamoResponseDTO> updateEstado(@PathVariable Long id, @RequestParam EstadoReclamo estado, @RequestParam String observacion) {
        Reclamo actualizado = reclamoService.updateEstado(id, estado, observacion);
        return ResponseEntity.ok(new ReclamoResponseDTO(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}