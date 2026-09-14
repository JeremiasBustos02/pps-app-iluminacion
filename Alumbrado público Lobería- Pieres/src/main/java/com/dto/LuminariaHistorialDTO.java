package com.dto;

import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LuminariaHistorialDTO {

    private Long luminariaId;
    private String tipo;
    private String estado;
    private String zona;

    private List<ReclamoHistorialEntry> reclamos;

    @Data
    public static class ReclamoHistorialEntry {
        private Long id;
        private String numeroSeguimiento;
        private String tipoReclamo;
        private EstadoReclamo estado;
        private LocalDateTime fecha;
        private List<ReparacionResumenDTO> observacionesCuadrilla;
    }
}
