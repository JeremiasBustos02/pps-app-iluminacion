package com.enums;

import java.util.Set;

public enum EstadoReclamo {
    PENDIENTE(Set.of("ASIGNADO", "RECHAZADO")),
    ASIGNADO(Set.of("ESPERA_EDEA", "ESPERA_MATERIAL", "RESUELTO", "PENDIENTE")),
    ESPERA_EDEA(Set.of("ASIGNADO")),
    // RF-18: bloqueado por falta de repuesto en stock
    ESPERA_MATERIAL(Set.of("ASIGNADO", "RESUELTO")),
    RESUELTO(Set.of("CERRADO", "ASIGNADO")),
    CERRADO(Set.of()),
    RECHAZADO(Set.of("PENDIENTE"));

    private final Set<String> transicionesValidas;

    EstadoReclamo(Set<String> transicionesValidas) {
        this.transicionesValidas = transicionesValidas;
    }

    // Un reclamo sigue activo hasta que se resuelve, se cierra o se rechaza (RF-05)
    public boolean esActivo() {
        return this == PENDIENTE || this == ASIGNADO || this == ESPERA_EDEA || this == ESPERA_MATERIAL;
    }

    public boolean puedeTransicionarA(EstadoReclamo destino) {
        return transicionesValidas.contains(destino.name());
    }
}
