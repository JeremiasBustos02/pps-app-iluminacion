package com.service;

import com.dto.ReporteDashboardDTO;
import com.dto.ReporteDashboardDTO.*;
import com.entity.Reclamo;
import com.entity.ReclamoHistorial;
import com.entity.Reparacion;
import com.enums.EstadoReclamo;
import com.enums.TipoMovimiento;
import com.repository.MovimientoStockRepository;
import com.repository.ReclamoHistorialRepository;
import com.repository.ReclamoRepository;
import com.repository.ReparacionRepository;
import com.repository.ReporteRows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// RF-21: indicadores del panel de reportes del Administrador
@Service
public class ReporteService {

    private static final LocalDateTime SIN_LIMITE_DESDE = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime SIN_LIMITE_HASTA = LocalDateTime.of(2999, 12, 31, 23, 59);
    private static final String SIN_ZONA = "Sin zona";

    @Autowired
    private ReclamoRepository reclamoRepository;

    @Autowired
    private ReclamoHistorialRepository reclamoHistorialRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Transactional(readOnly = true)
    public ReporteDashboardDTO getDashboard(LocalDate desde, LocalDate hasta) {
        ReporteDashboardDTO dto = new ReporteDashboardDTO();
        dto.setDesde(desde);
        dto.setHasta(hasta);
        dto.setReclamosPorZona(getReclamosPorZona(desde, hasta));
        dto.setTiempoResolucion(getTiempoResolucion(desde, hasta));
        dto.setReparaciones(getReparaciones(desde, hasta));
        dto.setMateriales(getMateriales(desde, hasta));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReclamosZonaInfo> getReclamosPorZona(LocalDate desde, LocalDate hasta) {
        Map<String, ReclamosZonaInfo> porZona = new TreeMap<>();
        for (ReporteRows.ReclamosPorZonaEstado fila : reclamoRepository.contarPorZonaYEstado(inicio(desde, hasta), fin(hasta))) {
            String zona = fila.zona() != null ? fila.zona() : SIN_ZONA;
            ReclamosZonaInfo info = porZona.computeIfAbsent(zona, z -> {
                ReclamosZonaInfo nueva = new ReclamosZonaInfo();
                nueva.setZona(z);
                return nueva;
            });
            long cantidad = fila.cantidad();
            info.setTotal(info.getTotal() + cantidad);
            if (fila.estado() == null || fila.estado().esActivo()) {
                info.setActivos(info.getActivos() + cantidad);
            } else if (fila.estado() == EstadoReclamo.RECHAZADO) {
                info.setRechazados(info.getRechazados() + cantidad);
            } else {
                info.setResueltos(info.getResueltos() + cantidad); // RESUELTO o CERRADO
            }
        }
        return porZona.values().stream()
                .sorted(Comparator.comparingLong(ReclamosZonaInfo::getTotal).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public TiempoResolucionInfo getTiempoResolucion(LocalDate desde, LocalDate hasta) {
        // Un reclamo puede reabrirse y resolverse de nuevo: se toma su última resolución del período
        Map<Long, ReclamoHistorial> resoluciones = reclamoHistorialRepository
                .findCambiosAEstadoEntre(EstadoReclamo.RESUELTO, inicio(desde, hasta), fin(hasta)).stream()
                .collect(Collectors.toMap(h -> h.getReclamo().getId(), h -> h,
                        (a, b) -> a.getFechaCambio().isAfter(b.getFechaCambio()) ? a : b));

        TiempoResolucionInfo info = new TiempoResolucionInfo();
        Map<String, List<Double>> horasPorTipo = new TreeMap<>();
        List<Double> horas = new ArrayList<>();
        long conPlazo = 0;
        long dentroDePlazo = 0;

        for (ReclamoHistorial resolucion : resoluciones.values()) {
            Reclamo reclamo = resolucion.getReclamo();
            if (reclamo.getFecha() == null) {
                continue;
            }
            double horasResolucion = horasDeResolucion(reclamo, resolucion.getFechaCambio());
            horas.add(horasResolucion);

            String tipo = reclamo.getTipoReclamo() != null ? reclamo.getTipoReclamo().getNombre() : "Sin tipo";
            horasPorTipo.computeIfAbsent(tipo, t -> new ArrayList<>()).add(horasResolucion);

            if (reclamo.getFechaLimite() != null) {
                conPlazo++;
                if (!resolucion.getFechaCambio().isAfter(reclamo.getFechaLimite())) {
                    dentroDePlazo++;
                }
            }
        }

        info.setReclamosResueltos(horas.size());
        info.setPromedioHoras(promedio(horas));
        info.setDentroDePlazo(dentroDePlazo);
        info.setPorcentajeDentroDePlazo(conPlazo == 0 ? null : redondear(dentroDePlazo * 100.0 / conPlazo));
        horasPorTipo.forEach((tipo, lista) ->
                info.getPorTipo().add(new TiempoPorTipoInfo(tipo, lista.size(), promedio(lista))));
        return info;
    }

    @Transactional(readOnly = true)
    public ReparacionesInfo getReparaciones(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = inicio(desde, hasta);
        LocalDateTime fin = fin(hasta);
        List<Reparacion> reparaciones = reparacionRepository.findByFechaBetween(inicio, fin);

        ReparacionesInfo info = new ReparacionesInfo();
        info.setTotal(reparaciones.size());
        info.setPorCuadrilla(reparacionRepository.contarPorCuadrilla(inicio, fin).stream()
                .map(f -> new ConteoInfo(f.cuadrilla(), f.cantidad()))
                .sorted(Comparator.comparingLong(ConteoInfo::getCantidad).reversed())
                .toList());

        DateTimeFormatter formatoMes = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> porMes = reparaciones.stream()
                .filter(r -> r.getFecha() != null)
                .collect(Collectors.groupingBy(r -> r.getFecha().format(formatoMes), TreeMap::new, Collectors.counting()));
        porMes.forEach((mes, cantidad) -> info.getPorMes().add(new ConteoInfo(mes, cantidad)));
        return info;
    }

    @Transactional(readOnly = true)
    public MaterialesInfo getMateriales(LocalDate desde, LocalDate hasta) {
        Map<Long, MaterialInfo> porMaterial = new LinkedHashMap<>();
        MaterialesInfo info = new MaterialesInfo();

        for (ReporteRows.MovimientosPorMaterial fila : movimientoStockRepository.resumenPorMaterial(inicio(desde, hasta), fin(hasta))) {
            MaterialInfo material = porMaterial.computeIfAbsent(fila.materialId(), id -> {
                MaterialInfo nuevo = new MaterialInfo();
                nuevo.setMaterialId(id);
                nuevo.setMaterial(fila.material());
                return nuevo;
            });
            long unidades = fila.cantidad() != null ? fila.cantidad() : 0;
            BigDecimal monto = fila.monto() != null ? fila.monto() : BigDecimal.ZERO;

            if (fila.tipo() == TipoMovimiento.EGRESO) {
                material.setUnidadesConsumidas(material.getUnidadesConsumidas() + unidades);
                material.setCostoConsumo(material.getCostoConsumo().add(monto));
                info.setCostoConsumo(info.getCostoConsumo().add(monto));
            } else {
                material.setUnidadesRepuestas(material.getUnidadesRepuestas() + unidades);
                material.setInversionReposicion(material.getInversionReposicion().add(monto));
                info.setInversionReposicion(info.getInversionReposicion().add(monto));
            }
        }

        info.setPorMaterial(porMaterial.values().stream()
                .sorted(Comparator.comparing(MaterialInfo::getCostoConsumo).reversed()
                        .thenComparing(Comparator.comparingLong(MaterialInfo::getUnidadesConsumidas).reversed()))
                .toList());
        return info;
    }

    // Tiempo desde el alta hasta la resolución, descontando las pausas por espera de EDEA (RF-17)
    double horasDeResolucion(Reclamo reclamo, LocalDateTime fechaResolucion) {
        long minutos = Duration.between(reclamo.getFecha(), fechaResolucion).toMinutes();
        int pausa = reclamo.getMinutosPausa() != null ? reclamo.getMinutosPausa() : 0;
        return Math.max(0, minutos - pausa) / 60.0;
    }

    private LocalDateTime inicio(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        return desde != null ? desde.atStartOfDay() : SIN_LIMITE_DESDE;
    }

    private LocalDateTime fin(LocalDate hasta) {
        return hasta != null ? hasta.atTime(LocalTime.MAX) : SIN_LIMITE_HASTA;
    }

    private Double promedio(List<Double> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        return redondear(valores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private Double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
