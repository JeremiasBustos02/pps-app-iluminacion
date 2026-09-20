package com.repository;

import com.enums.EstadoReclamo;

public interface ReclamoActivoView {
    Long getLuminariaId();
    EstadoReclamo getEstado();
    Integer getPrioridad();
}
