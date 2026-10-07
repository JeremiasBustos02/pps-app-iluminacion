package com.controller;

import com.dto.ReclamoDTO;
import com.dto.ReclamoPaqueteDTO;
import com.dto.ReclamoResponseDTO;
import com.entity.Reclamo;
import com.entity.Usuario;
import com.enums.EstadoReclamo;
import com.service.ReclamoService;
import com.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reclamos")
public class ReclamoController {

    @Autowired
    private ReclamoService reclamoService;

    @Autowired
    private UsuarioService usuarioService;

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

    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    @GetMapping("/{id}/paquete")
    public ResponseEntity<ReclamoPaqueteDTO> getPaquete(@PathVariable Long id) {
        return ResponseEntity.ok(reclamoService.getPaquete(id));
    }

    @GetMapping("/seguimiento/{numeroSeguimiento}")
    public ResponseEntity<ReclamoResponseDTO> getBySeguimiento(@PathVariable String numeroSeguimiento) {
        return reclamoService.findByNumeroSeguimiento(numeroSeguimiento)
                .map(r -> ResponseEntity.ok(new ReclamoResponseDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mis-reclamos")
    @PreAuthorize("hasRole('VECINO')")
    public ResponseEntity<List<ReclamoResponseDTO>> getMisReclamos() {
        String dni = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioAutenticado = usuarioService.findByDni(Long.valueOf(dni))
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        List<ReclamoResponseDTO> list = reclamoService.findByUsuario(usuarioAutenticado.getId()).stream()
                .map(ReclamoResponseDTO::new)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VECINO', 'ADMINISTRADOR')")
    public ResponseEntity<ReclamoResponseDTO> create(@RequestBody ReclamoDTO dto) {
        // El reclamo queda asociado a quien está autenticado (el "username" es el DNI, RF-03),
        // nunca al usuarioId que venga en el body: evita que un vecino genere reclamos a nombre de otro.
        String dni = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioAutenticado = usuarioService.findByDni(Long.valueOf(dni))
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
        dto.setUsuarioId(usuarioAutenticado.getId());

        Reclamo nuevoReclamo = reclamoService.saveFromDTO(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ReclamoResponseDTO(nuevoReclamo));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<ReclamoResponseDTO> updateEstado(@PathVariable Long id, @RequestParam EstadoReclamo estado, @RequestParam String observacion) {
        // RF-20: la resolución siempre pasa por el diagnóstico (RF-13) y las observaciones (RF-14)
        if (estado == EstadoReclamo.RESUELTO) {
            throw new IllegalArgumentException(
                    "Para resolver un reclamo registre la reparación con el diagnóstico y las observaciones");
        }
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