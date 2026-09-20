package com.dto;

import com.enums.ColorLuminaria;
import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// RF-05: detalle por clic, solo para Técnico/Administrador
@Data
public class LuminariaDetalleMapaDTO {
    private Long id;
    private ColorLuminaria color;
    private boolean marcaGris;
    private String zonaNombre;
    private Double latitud;
    private Double longitud;

    private String tecnologia;
    private String potencia;
    private String columna;
    private String estado;

    private List<ObservacionVecino> observacionesVecino;

    @Data
    public static class ObservacionVecino {
        private Long reclamoId;
        private String numeroSeguimiento;
        private String tipoReclamo;
        private Integer prioridad;
        private EstadoReclamo estado;
        private LocalDateTime fecha;
        private String observacion;
    }
}
