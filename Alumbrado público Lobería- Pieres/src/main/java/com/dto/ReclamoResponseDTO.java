package com.dto;

import com.entity.Reclamo;
import com.enums.EstadoReclamo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReclamoResponseDTO {
    private Long id;
    private String numeroSeguimiento;
    private EstadoReclamo estado;
    private LocalDateTime fecha;
    private Integer tiempoEstimado;
    // RF-17: con el SLA pausado (espera EDEA) no se informa fecha estimada al vecino
    private boolean slaPausado;
    private LocalDateTime fechaEstimadaResolucion;
    private Long luminariaId;
    private Long tipoReclamoId;
    private String tipoReclamoNombre;
    private Long usuarioId;
    private String usuarioNombre;

    public ReclamoResponseDTO(Reclamo reclamo) {
        this.id = reclamo.getId();
        this.numeroSeguimiento = reclamo.getNumeroSeguimiento();
        this.estado = reclamo.getEstado();
        this.fecha = reclamo.getFecha();
        this.tiempoEstimado = reclamo.getTiempoEstimado();
        this.slaPausado = reclamo.getSlaPausadoDesde() != null;
        this.fechaEstimadaResolucion = this.slaPausado ? null : reclamo.getFechaLimite();

        if (reclamo.getLuminaria() != null) {
            this.luminariaId = reclamo.getLuminaria().getId();
        }

        if (reclamo.getTipoReclamo() != null) {
            this.tipoReclamoId = reclamo.getTipoReclamo().getId();
            this.tipoReclamoNombre = reclamo.getTipoReclamo().getNombre();
        }

        if (reclamo.getUsuario() != null) {
            this.usuarioId = reclamo.getUsuario().getId();
            this.usuarioNombre = reclamo.getUsuario().getNombre();
        }
    }
}
