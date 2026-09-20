package com.controller;

import com.dto.LuminariaDTO;
import com.dto.LuminariaDetalleMapaDTO;
import com.dto.LuminariaMapaDTO;
import com.dto.LuminariaHistorialDTO;
import com.dto.LuminariaResponseDTO;
import com.dto.ReclamoResponseDTO;
import com.entity.Luminaria;
import com.enums.EstadoReclamo;
import com.service.LuminariaService;
import com.service.MapaLuminariaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/luminarias")
public class LuminariaController {

    @Autowired
    private LuminariaService luminariaService;

    @Autowired
    private MapaLuminariaService mapaLuminariaService;

    @GetMapping
    public ResponseEntity<List<LuminariaResponseDTO>> getAll(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long zonaId) {
        List<LuminariaResponseDTO> list = luminariaService.filtrar(estado, zonaId).stream()
                .map(LuminariaResponseDTO::new)
                .map(this::ocultarDatosTecnicosSiVecino)
                .toList();
        return ResponseEntity.ok(list);
    }

    // RF-05: puntos del mapa con color de estado y marca gris; visible para todos los roles
    @GetMapping("/mapa")
    public ResponseEntity<List<LuminariaMapaDTO>> getMapa(@RequestParam(required = false) Long zonaId) {
        return ResponseEntity.ok(mapaLuminariaService.getMapa(zonaId));
    }

    // RF-05: detalle por clic (observación del vecino y especificaciones técnicas), oculto para el Vecino
    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<LuminariaDetalleMapaDTO> getDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(mapaLuminariaService.getDetalle(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LuminariaResponseDTO> getById(@PathVariable Long id) {
        return luminariaService.findById(id)
                .map(LuminariaResponseDTO::new)
                .map(this::ocultarDatosTecnicosSiVecino)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('TECNICO', 'ADMINISTRADOR')")
    public ResponseEntity<LuminariaHistorialDTO> getHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(luminariaService.getHistorial(id));
    }

    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<LuminariaResponseDTO>> getByZona(@PathVariable Long zonaId) {
        List<LuminariaResponseDTO> list = luminariaService.findByZona(zonaId).stream()
                .map(luminaria -> new LuminariaResponseDTO(luminaria))
                .map(this::ocultarDatosTecnicosSiVecino)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<LuminariaResponseDTO> create(@RequestBody LuminariaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new LuminariaResponseDTO(luminariaService.save(dto)));
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

    // RF-05: las especificaciones técnicas (tecnología, potencia, columna) no se exponen al rol Vecino
    private LuminariaResponseDTO ocultarDatosTecnicosSiVecino(LuminariaResponseDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean personalTecnico = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECNICO") || a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        if (!personalTecnico) {
            dto.setTipo(null);
            dto.setPotencia(null);
            dto.setColumna(null);
        }
        return dto;
    }
}
