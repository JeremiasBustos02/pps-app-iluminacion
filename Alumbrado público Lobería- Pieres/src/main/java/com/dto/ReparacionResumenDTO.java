package com.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReparacionResumenDTO {
    private Long id;
    private String observacion;
    private LocalDateTime fecha;
    private List<String> tecnicos;
    private List<String> componentesAveriados;
}
