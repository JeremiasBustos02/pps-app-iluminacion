package com.entity;

import jakarta.persistence.*;
        import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class ReparacionComponente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reparacion_id", nullable = false)
    private Reparacion reparacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "componente_id", nullable = false)
    private Componente componente;

    @Column(name = "estado_componente")
    private String estadoComponente; // Ej: QUEMADO, ROTO, SULFATADO

    private String observacion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}