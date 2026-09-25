package com.controller;

import com.dto.HojaDelDiaDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.entity.HojaDeRuta;
import com.entity.Usuario;
import com.service.HojaDeRutaService;
import com.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hojas-de-ruta")
public class HojaDeRutaController {

    @Autowired
    private HojaDeRutaService hojaDeRutaService;
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<HojaDeRuta>> getAll() {
        return ResponseEntity.ok(hojaDeRutaService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HojaDeRuta> getById(@PathVariable Long id) {
        return hojaDeRutaService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HojaDeRuta> create(@RequestBody HojaDeRuta hojaDeRuta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hojaDeRutaService.save(hojaDeRuta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hojaDeRutaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mi-hoja-del-dia")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<List<HojaDelDiaDTO>> getMiHojaDelDia() {
        return ResponseEntity.ok(hojaDeRutaService.getMiHojaDelDia(usuarioAutenticado().getId()));
    }

    // RF-20: el técnico marca como atendido un reclamo de su hoja del día cargando el diagnóstico
    // (componentes rotos, RF-13), observaciones (RF-14) y materiales usados (RF-15)
    @PostMapping("/mi-hoja-del-dia/reclamos/{reclamoId}/atender")
    @PreAuthorize("hasRole('TECNICO')")
    public ResponseEntity<ReparacionResponseDTO> atenderReclamo(@PathVariable Long reclamoId,
                                                                @RequestBody ReparacionDTO diagnostico) {
        ReparacionResponseDTO resultado =
                hojaDeRutaService.atenderReclamo(usuarioAutenticado().getId(), reclamoId, diagnostico);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    private Usuario usuarioAutenticado() {
        String dni = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.findByDni(Long.valueOf(dni))
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));
    }
}
