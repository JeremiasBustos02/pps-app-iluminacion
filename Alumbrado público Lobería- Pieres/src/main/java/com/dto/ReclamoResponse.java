package com.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReclamoResponse {
    private Long id;
    private String numeroSeguimiento;
    private String estado;
    private LocalDateTime fecha;
    private Integer tiempoEstimado;
    private Long luminariaId;
    private Long tipoReclamoId;
    private String tipoReclamoNombre;
    private Long usuarioId;
}
