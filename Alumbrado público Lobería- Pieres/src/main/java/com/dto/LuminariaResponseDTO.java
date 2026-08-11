package com.dto;

import com.entity.Luminaria;
import lombok.Data;

@Data
public class LuminariaResponseDTO {
    private Long id;
    private String potencia;
    private String columna;
    private String tipo;
    private String estado;
    private Long zonaId;
    private String zonaNombre;
    private Double latitud;
    private Double longitud;


    public LuminariaResponseDTO(Luminaria luminaria) {
        this.id = luminaria.getId();
        this.potencia = luminaria.getPotencia();
        this.columna = luminaria.getColumna();
        this.tipo = luminaria.getTipo();
        this.estado = luminaria.getEstado();

        if (luminaria.getZona() != null) {
            this.zonaId = luminaria.getZona().getId();
            this.zonaNombre = luminaria.getZona().getNombre();
        }

        if (luminaria.getCoordenadas() != null) {
            this.latitud = luminaria.getCoordenadas().getY();
            this.longitud = luminaria.getCoordenadas().getX();
        }
    }
}
