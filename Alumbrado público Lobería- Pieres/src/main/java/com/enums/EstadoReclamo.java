package com.enums;

import java.util.Set;

public enum EstadoReclamo {
    PENDIENTE(Set.of("ASIGNADO", "RECHAZADO")),
    ASIGNADO(Set.of("ESPERA_EDEA", "RESUELTO", "PENDIENTE")),
    ESPERA_EDEA(Set.of("ASIGNADO")),
    RESUELTO(Set.of("CERRADO", "ASIGNADO")),
    CERRADO(Set.of()),
    RECHAZADO(Set.of("PENDIENTE"));

    private final Set<String> transicionesValidas;

    EstadoReclamo(Set<String> transicionesValidas) {
        this.transicionesValidas = transicionesValidas;
    }

    public boolean puedeTransicionarA(EstadoReclamo destino) {
        return transicionesValidas.contains(destino.name());
    }
}
