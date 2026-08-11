package com.controller;

import com.entity.ReparacionTecnico;
import com.service.ReparacionTecnicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reparacion-tecnicos")
public class ReparacionTecnicoController {

    @Autowired
    private ReparacionTecnicoService reparacionTecnicoService;

    @GetMapping("/reparacion/{reparacionId}")
    public ResponseEntity<List<ReparacionTecnico>> getByReparacion(@PathVariable Long reparacionId) {
        return ResponseEntity.ok(reparacionTecnicoService.findByReparacion(reparacionId));
    }

    @PostMapping
    public ResponseEntity<ReparacionTecnico> assign(@RequestBody ReparacionTecnico reparacionTecnico) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reparacionTecnicoService.assignTecnico(reparacionTecnico));
    }
}