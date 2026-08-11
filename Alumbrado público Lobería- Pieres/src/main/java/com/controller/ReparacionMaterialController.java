package com.controller;

import com.entity.ReparacionMaterial;
import com.service.ReparacionMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reparacion-materiales")
public class ReparacionMaterialController {

    @Autowired
    private ReparacionMaterialService reparacionMaterialService;

    @GetMapping("/reparacion/{reparacionId}")
    public ResponseEntity<List<ReparacionMaterial>> getByReparacion(@PathVariable Long reparacionId) {
        return ResponseEntity.ok(reparacionMaterialService.findByReparacion(reparacionId));
    }

    @PostMapping
    public ResponseEntity<ReparacionMaterial> addMaterial(@RequestBody ReparacionMaterial reparacionMaterial) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reparacionMaterialService.addMaterial(reparacionMaterial));
    }
}