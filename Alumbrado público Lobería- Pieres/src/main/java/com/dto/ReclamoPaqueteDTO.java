package com.dto;

import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReclamoPaqueteDTO {

    // Datos del reclamo
    private Long id;
    private String numeroSeguimiento;
    private EstadoReclamo estado;
    private LocalDateTime fecha;
    private Integer tiempoEstimado;
    // RF-17: con el SLA pausado (espera EDEA) no se informa fecha estimada
    private boolean slaPausado;
    private LocalDateTime fechaEstimadaResolucion;
    private Integer minutosPausa;

    // Bloques anidados
    private TipoReclamoInfo tipoReclamo;
    private VecinoInfo vecino;
    private LuminariaInfo luminaria;
    private List<HistorialInfo> historial;
    private List<ReparacionResumenDTO> observacionesCuadrilla;
    // RF-18: repuestos del último diagnóstico y si el reclamo está bloqueado por falta de material
    private DisponibilidadMaterialesDTO disponibilidadMateriales;

    // --- DTOs internos ---

    @Data
    public static class TipoReclamoInfo {
        private Long id;
        private String nombre;
        private Integer prioridad;
    }

    @Data
    public static class VecinoInfo {
        private Long id;
        private String nombre;
        private Long dni;
        private String email;
    }

    @Data
    public static class LuminariaInfo {
        private Long id;
        private String tipo;
        private String estado;
        private String zona;
    }

    @Data
    public static class HistorialInfo {
        private EstadoReclamo estadoAnterior;
        private EstadoReclamo estadoNuevo;
        private String observacion;
        private LocalDateTime fechaCambio;
    }
}
