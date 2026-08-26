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

    @Column(name = "tiempo_estimado")
    private Integer tiempoEstimado;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}