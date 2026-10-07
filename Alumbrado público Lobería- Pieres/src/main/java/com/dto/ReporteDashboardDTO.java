package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// RF-21: panel de reportes e indicadores del Administrador
@Data
public class ReporteDashboardDTO {

    // Período consultado (null = sin límite)
    private LocalDate desde;
    private LocalDate hasta;

    private List<ReclamosZonaInfo> reclamosPorZona = new ArrayList<>();
    private TiempoResolucionInfo tiempoResolucion;
    private ReparacionesInfo reparaciones;
    private MaterialesInfo materiales;

    @Data
    public static class ReclamosZonaInfo {
        private String zona;
        private long total;
        private long activos;
        private long resueltos;
        private long rechazados;
    }

    @Data
    public static class TiempoResolucionInfo {
        private long reclamosResueltos;
        // Horas desde el alta hasta la resolución, sin contar el tiempo en espera de EDEA (RF-17)
        private Double promedioHoras;
        private long dentroDePlazo;
        private Double porcentajeDentroDePlazo;
        private List<TiempoPorTipoInfo> porTipo = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TiempoPorTipoInfo {
        private String tipoReclamo;
        private long resueltos;
        private Double promedioHoras;
    }

    @Data
    public static class ReparacionesInfo {
        private long total;
        private List<ConteoInfo> porCuadrilla = new ArrayList<>();
        private List<ConteoInfo> porMes = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConteoInfo {
        private String nombre;
        private long cantidad;
    }

    @Data
    public static class MaterialesInfo {
        // Valor de los materiales usados en reparaciones (egresos)
        private BigDecimal costoConsumo = BigDecimal.ZERO;
        // Valor de las reposiciones de stock (ingresos)
        private BigDecimal inversionReposicion = BigDecimal.ZERO;
        private List<MaterialInfo> porMaterial = new ArrayList<>();
    }

    @Data
    public static class MaterialInfo {
        private Long materialId;
        private String material;
        private long unidadesConsumidas;
        private BigDecimal costoConsumo = BigDecimal.ZERO;
        private long unidadesRepuestas;
        private BigDecimal inversionReposicion = BigDecimal.ZERO;
    }
}
