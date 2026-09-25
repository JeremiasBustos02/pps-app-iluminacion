package com.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import com.enums.EstadoReclamo;

@Data
@Entity
public class Reclamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_seguimiento", unique = true, nullable = false)
    private String numeroSeguimiento;

    @ManyToOne
    @JoinColumn(name = "luminaria_id")
    private Luminaria luminaria;

    @ManyToOne
    @JoinColumn(name = "tipo_reclamo_id")
    private TipoReclamo tipoReclamo;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReclamo estado = EstadoReclamo.PENDIENTE;

    private LocalDateTime fecha;

    // Observación libre del vecino al reportar (RF-05: se muestra en el detalle del mapa a Técnico/Admin)
    @Column(length = 500)
    private String observacion;

    @Column(name = "tiempo_estimado")
    private Integer tiempoEstimado;

    // RF-17: plazo de resolución; se corre hacia adelante por el tiempo que el reclamo pasó en ESPERA_EDEA
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    // RF-17: inicio de la pausa del SLA (null si el SLA está corriendo)
    @Column(name = "sla_pausado_desde")
    private LocalDateTime slaPausadoDesde;

    @Column(name = "minutos_pausa", nullable = false)
    private Integer minutosPausa = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}