package com.dto;

import lombok.Data;

@Data
public class LuminariaDTO {
    private String potencia;
    private String columna;
    private String tipo;
    private String estado;
    private Long zonaId;
    private Double latitud;
    private Double longitud;
}