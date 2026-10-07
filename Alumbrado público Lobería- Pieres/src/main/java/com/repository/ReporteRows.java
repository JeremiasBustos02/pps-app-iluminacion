package com.repository;

import com.enums.EstadoReclamo;
import com.enums.TipoMovimiento;

import java.math.BigDecimal;

// RF-21: filas de las consultas de agregación del panel de reportes
public final class ReporteRows {

    private ReporteRows() {
    }

    public record ReclamosPorZonaEstado(String zona, EstadoReclamo estado, Long cantidad) {
    }

    public record ReparacionesPorCuadrilla(String cuadrilla, Long cantidad) {
    }

    public record MovimientosPorMaterial(Long materialId, String material, TipoMovimiento tipo,
                                         Long cantidad, BigDecimal monto) {
    }
}
