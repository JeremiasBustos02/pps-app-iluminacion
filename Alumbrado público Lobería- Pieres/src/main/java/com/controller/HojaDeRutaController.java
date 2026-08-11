package com.controller;

import com.entity.HojaDeRuta;
import com.service.HojaDeRutaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hojas-de-ruta")
public class HojaDeRutaController {

    @Autowired
    private HojaDeRutaService hojaDeRutaService;

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
}