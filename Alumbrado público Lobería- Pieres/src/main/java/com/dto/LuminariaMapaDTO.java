package com.dto;

import com.enums.ColorLuminaria;
import lombok.Data;

// RF-05: datos mínimos de cada punto del mapa; visible para todos los roles (sin especificaciones técnicas)
@Data
public class LuminariaMapaDTO {
    private Long id;
    private Double latitud;
    private Double longitud;
    private ColorLuminaria color;
    private boolean marcaGris;
    private Long zonaId;
    private String zonaNombre;
}
