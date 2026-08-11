package com.controller;

import com.entity.HojaDeRutaReclamo;
import com.service.HojaDeRutaReclamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hoja-ruta-reclamos")
public class HojaDeRutaReclamoController {

    @Autowired
    private HojaDeRutaReclamoService hojaDeRutaReclamoService;

    @GetMapping("/hoja/{hojaDeRutaId}")
    public ResponseEntity<List<HojaDeRutaReclamo>> getByHoja(@PathVariable Long hojaDeRutaId) {
        return ResponseEntity.ok(hojaDeRutaReclamoService.findByHojaDeRuta(hojaDeRutaId));
    }

    @PostMapping
    public ResponseEntity<HojaDeRutaReclamo> addReclamo(@RequestBody HojaDeRutaReclamo hojaDeRutaReclamo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hojaDeRutaReclamoService.addReclamoToHoja(hojaDeRutaReclamo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeReclamo(@PathVariable Long id) {
        hojaDeRutaReclamoService.removeReclamoFromHoja(id);
        return ResponseEntity.noContent().build();
    }
}