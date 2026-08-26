package com.entity;

import com.enums.EstadoReclamo;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reclamo_historial")
public class ReclamoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamo_id", nullable = false)
    private Reclamo reclamo;

    // El estado anterior puede ser nulo (ej: cuando el reclamo recién nace)
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoReclamo estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoReclamo estadoNuevo;

    private String observacion;

    @Column(name = "fecha_cambio", updatable = false)
    private LocalDateTime fechaCambio = LocalDateTime.now();
}