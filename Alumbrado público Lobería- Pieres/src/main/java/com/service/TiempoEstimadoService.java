package com.service;

import com.entity.Luminaria;
import com.entity.TipoReclamo;
import com.enums.EstadoReclamo;
import com.repository.CuadrillaRepository;
import com.repository.ReclamoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// RF-10: tiempo estimado de resolución según prioridad, zona y carga de trabajo de las cuadrillas
@Service
public class TiempoEstimadoService {

    // Reclamos que una cuadrilla puede atender en una jornada
    static final int RECLAMOS_POR_CUADRILLA_POR_DIA = 8;

    // Traslado a zonas fuera del Área Urbana (Napaleufú, Paraje Dos Naciones)
    static final int HORAS_EXTRA_ZONA_RURAL = 24;

    // RF-18: plazo que se suma cuando el reclamo queda bloqueado por falta de repuesto
    public static final int HORAS_REPOSICION_MATERIAL = 72;

    // ESPERA_EDEA y ESPERA_MATERIAL no cuentan: esos reclamos no ocupan a las cuadrillas mientras esperan
    private static final List<EstadoReclamo> ESTADOS_EN_COLA =
            List.of(EstadoReclamo.PENDIENTE, EstadoReclamo.ASIGNADO);

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private CuadrillaRepository cuadrillaRepository;

    public Integer calcular(TipoReclamo tipoReclamo, Luminaria luminaria) {
        if (tipoReclamo == null || tipoReclamo.getPrioridad() == null) {
            return null;
        }
        int prioridad = tipoReclamo.getPrioridad();
        return horasBase(prioridad) + horasPorZona(luminaria) + horasPorCarga(prioridad);
    }

    int horasBase(int prioridad) {
        return switch (prioridad) {
            case 3 -> 24;   // alta: 24hs
            case 2 -> 72;   // media: 3 días
            default -> 168; // baja: 7 días
        };
    }

    int horasPorZona(Luminaria luminaria) {
        if (luminaria != null && luminaria.getZona() != null && !luminaria.getZona().isAreaUrbana()) {
            return HORAS_EXTRA_ZONA_RURAL;
        }
        return 0;
    }

    // Los reclamos activos de igual o mayor prioridad se atienden antes; cada jornada completa
    // de cola (según la cantidad de cuadrillas activas) suma un día al plazo
    int horasPorCarga(int prioridad) {
        long enCola = reclamoRepository.contarEnColaConPrioridadMinima(ESTADOS_EN_COLA, prioridad);
        int cuadrillas = Math.max(1, cuadrillaRepository.findByDeletedAtIsNull().size());
        long capacidadDiaria = (long) cuadrillas * RECLAMOS_POR_CUADRILLA_POR_DIA;
        return (int) (enCola / capacidadDiaria) * 24;
    }
}
