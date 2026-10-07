package com.dto;

import com.entity.Reparacion;
import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReparacionResponseDTO {
    private Long id;
    private String observacion;
    private LocalDateTime fecha;
    private Long reclamoId;
    private String reclamoNumeroSeguimiento;
    private EstadoReclamo estadoReclamo;
    // RF-18: se completa al registrar la reparación (diagnóstico)
    private DisponibilidadMaterialesDTO disponibilidadMateriales;

    public ReparacionResponseDTO(Reparacion reparacion) {
        this.id = reparacion.getId();
        this.observacion = reparacion.getObservacion();
        this.fecha = reparacion.getFecha();

        if (reparacion.getReclamo() != null) {
            this.reclamoId = reparacion.getReclamo().getId();
            this.reclamoNumeroSeguimiento = reparacion.getReclamo().getNumeroSeguimiento();
            this.estadoReclamo = reparacion.getReclamo().getEstado();
        }
    }
}
