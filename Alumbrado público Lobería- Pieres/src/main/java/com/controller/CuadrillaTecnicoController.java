package com.controller;

import com.entity.CuadrillaTecnico;
import com.service.CuadrillaTecnicoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuadrilla-tecnicos")
public class CuadrillaTecnicoController {

    @Autowired
    private CuadrillaTecnicoService cuadrillaTecnicoService;

    @GetMapping("/cuadrilla/{cuadrillaId}")
    public ResponseEntity<List<CuadrillaTecnico>> getByCuadrilla(@PathVariable Long cuadrillaId) {
        return ResponseEntity.ok(cuadrillaTecnicoService.findByCuadrilla(cuadrillaId));
    }

    @PostMapping
    public ResponseEntity<CuadrillaTecnico> assign(@RequestBody CuadrillaTecnico cuadrillaTecnico) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuadrillaTecnicoService.assignTecnico(cuadrillaTecnico));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        cuadrillaTecnicoService.removeTecnico(id);
        return ResponseEntity.noContent().build();
    }
}