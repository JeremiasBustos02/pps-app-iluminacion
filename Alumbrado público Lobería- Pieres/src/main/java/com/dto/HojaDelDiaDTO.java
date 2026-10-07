package com.dto;

import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// RF-20: hoja de ruta del día del técnico con los reclamos a atender
@Data
public class HojaDelDiaDTO {

    private Long id;
    private LocalDateTime fecha;
    private Long cuadrillaId;
    private String cuadrilla;
    private List<ReclamoEnHojaInfo> reclamos = new ArrayList<>();

    @Data
    public static class ReclamoEnHojaInfo {
        private Long id;
        private String numeroSeguimiento;
        private EstadoReclamo estado;
        private String tipoReclamo;
        private Integer prioridad;
        private Long luminariaId;
        private String zona;
        private String observacionVecino;
        private LocalDateTime fechaLimite;
        // false cuando el reclamo ya fue resuelto, cerrado o rechazado
        private boolean pendienteDeAtencion;
    }
}
