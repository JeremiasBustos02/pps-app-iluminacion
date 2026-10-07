package com.controller;

import com.dto.ReporteDashboardDTO;
import com.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// RF-21: panel de reportes e indicadores (solo Administrador).
// Todos los endpoints aceptan un período opcional: ?desde=2026-01-01&hasta=2026-06-30
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/dashboard")
    public ResponseEntity<ReporteDashboardDTO> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.getDashboard(desde, hasta));
    }

    @GetMapping("/reclamos-por-zona")
    public ResponseEntity<List<ReporteDashboardDTO.ReclamosZonaInfo>> getReclamosPorZona(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.getReclamosPorZona(desde, hasta));
    }

    @GetMapping("/tiempo-resolucion")
    public ResponseEntity<ReporteDashboardDTO.TiempoResolucionInfo> getTiempoResolucion(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.getTiempoResolucion(desde, hasta));
    }

    @GetMapping("/reparaciones")
    public ResponseEntity<ReporteDashboardDTO.ReparacionesInfo> getReparaciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.getReparaciones(desde, hasta));
    }

    @GetMapping("/materiales")
    public ResponseEntity<ReporteDashboardDTO.MaterialesInfo> getMateriales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.getMateriales(desde, hasta));
    }
}
