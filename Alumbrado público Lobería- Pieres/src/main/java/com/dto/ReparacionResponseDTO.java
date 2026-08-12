package com.dto;

import com.entity.Reparacion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReparacionResponseDTO {
    private Long id;
    private String observacion;
    private LocalDateTime fecha;
    private Long reclamoId;
    private String reclamoNumeroSeguimiento;

    public ReparacionResponseDTO(Reparacion reparacion) {
        this.id = reparacion.getId();
        this.observacion = reparacion.getObservacion();
        this.fecha = reparacion.getFecha();

        if (reparacion.getReclamo() != null) {
            this.reclamoId = reparacion.getReclamo().getId();
            this.reclamoNumeroSeguimiento = reparacion.getReclamo().getNumeroSeguimiento();
        }
    }
}
