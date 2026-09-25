package com.service;

import com.dto.ReporteDashboardDTO;
import com.entity.Reclamo;
import com.entity.ReclamoHistorial;
import com.entity.Reparacion;
import com.entity.TipoReclamo;
import com.enums.EstadoReclamo;
import com.enums.TipoMovimiento;
import com.repository.MovimientoStockRepository;
import com.repository.ReclamoHistorialRepository;
import com.repository.ReclamoRepository;
import com.repository.ReparacionRepository;
import com.repository.ReporteRows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReporteService: RF-21 (panel de reportes e indicadores)")
class ReporteServiceTest {

    @Mock private ReclamoRepository reclamoRepository;
    @Mock private ReclamoHistorialRepository reclamoHistorialRepository;
    @Mock private ReparacionRepository reparacionRepository;
    @Mock private MovimientoStockRepository movimientoStockRepository;

    @InjectMocks private ReporteService reporteService;

    @Test
    @DisplayName("agrupa los reclamos por zona separando activos, resueltos y rechazados")
    void reclamosPorZona() {
        when(reclamoRepository.contarPorZonaYEstado(any(), any())).thenReturn(List.of(
                new ReporteRows.ReclamosPorZonaEstado("Lobería", EstadoReclamo.PENDIENTE, 4L),
                new ReporteRows.ReclamosPorZonaEstado("Lobería", EstadoReclamo.RESUELTO, 3L),
                new ReporteRows.ReclamosPorZonaEstado("Lobería", EstadoReclamo.CERRADO, 2L),
                new ReporteRows.ReclamosPorZonaEstado("Pieres", EstadoReclamo.RECHAZADO, 1L),
                new ReporteRows.ReclamosPorZonaEstado(null, EstadoReclamo.ESPERA_EDEA, 1L)));

        List<ReporteDashboardDTO.ReclamosZonaInfo> zonas = reporteService.getReclamosPorZona(null, null);

        assertThat(zonas).extracting(ReporteDashboardDTO.ReclamosZonaInfo::getZona)
                .containsExactly("Lobería", "Pieres", "Sin zona");
        ReporteDashboardDTO.ReclamosZonaInfo loberia = zonas.get(0);
        assertThat(loberia.getTotal()).isEqualTo(9);
        assertThat(loberia.getActivos()).isEqualTo(4);
        assertThat(loberia.getResueltos()).isEqualTo(5);
        assertThat(zonas.get(1).getRechazados()).isEqualTo(1);
        assertThat(zonas.get(2).getActivos()).isEqualTo(1);
    }

    private ReclamoHistorial resolucion(long reclamoId, String tipo, LocalDateTime alta, LocalDateTime resuelto,
                                        LocalDateTime fechaLimite, int minutosPausa) {
        TipoReclamo tipoReclamo = new TipoReclamo();
        tipoReclamo.setNombre(tipo);
        Reclamo reclamo = new Reclamo();
        reclamo.setId(reclamoId);
        reclamo.setTipoReclamo(tipoReclamo);
        reclamo.setFecha(alta);
        reclamo.setFechaLimite(fechaLimite);
        reclamo.setMinutosPausa(minutosPausa);
        ReclamoHistorial h = new ReclamoHistorial();
        h.setReclamo(reclamo);
        h.setEstadoNuevo(EstadoReclamo.RESUELTO);
        h.setFechaCambio(resuelto);
        return h;
    }

    @Test
    @DisplayName("calcula el tiempo promedio de resolución descontando la espera de EDEA y el cumplimiento de plazo")
    void tiempoPromedioDeResolucion() {
        LocalDateTime alta = LocalDateTime.of(2026, 9, 1, 8, 0);
        when(reclamoHistorialRepository.findCambiosAEstadoEntre(eq(EstadoReclamo.RESUELTO), any(), any())).thenReturn(List.of(
                // 10 h, dentro del plazo
                resolucion(1L, "Luminaria apagada", alta, alta.plusHours(10), alta.plusHours(24), 0),
                // 50 h totales con 20 h en EDEA = 30 h, fuera del plazo
                resolucion(2L, "Luminaria apagada", alta, alta.plusHours(50), alta.plusHours(24), 20 * 60),
                // 20 h
                resolucion(3L, "Cableado expuesto", alta, alta.plusHours(20), null, 0)));

        ReporteDashboardDTO.TiempoResolucionInfo info = reporteService.getTiempoResolucion(null, null);

        assertThat(info.getReclamosResueltos()).isEqualTo(3);
        assertThat(info.getPromedioHoras()).isEqualTo(20.0);
        assertThat(info.getDentroDePlazo()).isEqualTo(1);
        assertThat(info.getPorcentajeDentroDePlazo()).isEqualTo(50.0);
        assertThat(info.getPorTipo())
                .extracting(t -> t.getTipoReclamo() + "=" + t.getResueltos() + "/" + t.getPromedioHoras())
                .containsExactly("Cableado expuesto=1/20.0", "Luminaria apagada=2/20.0");
    }

    @Test
    @DisplayName("un reclamo resuelto dos veces cuenta una sola vez, con su última resolución")
    void reclamoReabiertoCuentaUnaVez() {
        LocalDateTime alta = LocalDateTime.of(2026, 9, 1, 8, 0);
        when(reclamoHistorialRepository.findCambiosAEstadoEntre(eq(EstadoReclamo.RESUELTO), any(), any())).thenReturn(List.of(
                resolucion(1L, "Otro", alta, alta.plusHours(5), null, 0),
                resolucion(1L, "Otro", alta, alta.plusHours(30), null, 0)));

        ReporteDashboardDTO.TiempoResolucionInfo info = reporteService.getTiempoResolucion(null, null);

        assertThat(info.getReclamosResueltos()).isEqualTo(1);
        assertThat(info.getPromedioHoras()).isEqualTo(30.0);
        assertThat(info.getPorcentajeDentroDePlazo()).isNull();
    }

    @Test
    @DisplayName("cuenta las reparaciones por cuadrilla y por mes")
    void reparacionesPorCuadrillaYMes() {
        Reparacion agosto = new Reparacion();
        agosto.setFecha(LocalDateTime.of(2026, 8, 20, 10, 0));
        Reparacion septiembre1 = new Reparacion();
        septiembre1.setFecha(LocalDateTime.of(2026, 9, 2, 10, 0));
        Reparacion septiembre2 = new Reparacion();
        septiembre2.setFecha(LocalDateTime.of(2026, 9, 15, 10, 0));
        when(reparacionRepository.findByFechaBetween(any(), any())).thenReturn(List.of(septiembre1, agosto, septiembre2));
        when(reparacionRepository.contarPorCuadrilla(any(), any())).thenReturn(List.of(
                new ReporteRows.ReparacionesPorCuadrilla("Cuadrilla Sur", 1L),
                new ReporteRows.ReparacionesPorCuadrilla("Cuadrilla Norte", 2L)));

        ReporteDashboardDTO.ReparacionesInfo info = reporteService.getReparaciones(null, null);

        assertThat(info.getTotal()).isEqualTo(3);
        assertThat(info.getPorCuadrilla()).extracting(ReporteDashboardDTO.ConteoInfo::getNombre)
                .containsExactly("Cuadrilla Norte", "Cuadrilla Sur");
        assertThat(info.getPorMes()).extracting(m -> m.getNombre() + "=" + m.getCantidad())
                .containsExactly("2026-08=1", "2026-09=2");
    }

    @Test
    @DisplayName("suma consumo (egresos) e inversión (ingresos) por material")
    void consumoEInversionEnMateriales() {
        when(movimientoStockRepository.resumenPorMaterial(any(), any())).thenReturn(List.of(
                new ReporteRows.MovimientosPorMaterial(1L, "Lámpara LED", TipoMovimiento.EGRESO, 10L, new BigDecimal("50000.00")),
                new ReporteRows.MovimientosPorMaterial(1L, "Lámpara LED", TipoMovimiento.INGRESO, 20L, new BigDecimal("100000.00")),
                new ReporteRows.MovimientosPorMaterial(2L, "Cinta aisladora", TipoMovimiento.EGRESO, 4L, null)));

        ReporteDashboardDTO.MaterialesInfo info = reporteService.getMateriales(null, null);

        assertThat(info.getCostoConsumo()).isEqualByComparingTo("50000");
        assertThat(info.getInversionReposicion()).isEqualByComparingTo("100000");
        assertThat(info.getPorMaterial()).hasSize(2);
        ReporteDashboardDTO.MaterialInfo lampara = info.getPorMaterial().get(0);
        assertThat(lampara.getMaterial()).isEqualTo("Lámpara LED");
        assertThat(lampara.getUnidadesConsumidas()).isEqualTo(10);
        assertThat(lampara.getUnidadesRepuestas()).isEqualTo(20);
        // material sin precio cargado: cuenta unidades pero no suma monto
        assertThat(info.getPorMaterial().get(1).getUnidadesConsumidas()).isEqualTo(4);
        assertThat(info.getPorMaterial().get(1).getCostoConsumo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("aplica el período consultado a las consultas")
    void aplicaPeriodo() {
        reporteService.getReclamosPorZona(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        verify(reclamoRepository).contarPorZonaYEstado(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDate.of(2026, 6, 30).atTime(java.time.LocalTime.MAX));
    }

    @Test
    @DisplayName("rechaza un período con desde posterior a hasta")
    void periodoInvalido() {
        assertThatThrownBy(() -> reporteService.getDashboard(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1)))
                .hasMessageContaining("desde");
    }
}
