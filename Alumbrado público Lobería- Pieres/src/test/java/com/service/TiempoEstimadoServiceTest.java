package com.service;

import com.entity.Cuadrilla;
import com.entity.Luminaria;
import com.entity.TipoReclamo;
import com.entity.Zona;
import com.repository.CuadrillaRepository;
import com.repository.ReclamoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TiempoEstimadoService: RF-10 (tiempo estimado por prioridad, zona y carga)")
class TiempoEstimadoServiceTest {

    @Mock private ReclamoRepository reclamoRepository;
    @Mock private CuadrillaRepository cuadrillaRepository;

    @InjectMocks private TiempoEstimadoService tiempoEstimadoService;

    private TipoReclamo tipo(int prioridad) {
        TipoReclamo tipo = new TipoReclamo();
        tipo.setPrioridad(prioridad);
        return tipo;
    }

    private Luminaria luminariaEn(boolean areaUrbana) {
        Zona zona = new Zona();
        zona.setAreaUrbana(areaUrbana);
        Luminaria luminaria = new Luminaria();
        luminaria.setZona(zona);
        return luminaria;
    }

    private void cargaDeTrabajo(int prioridad, long reclamosEnCola, int cuadrillas) {
        when(reclamoRepository.contarEnColaConPrioridadMinima(anyList(), eq(prioridad))).thenReturn(reclamosEnCola);
        when(cuadrillaRepository.findByDeletedAtIsNull())
                .thenReturn(Collections.nCopies(cuadrillas, new Cuadrilla()));
    }

    @Test
    @DisplayName("sin cola y en área urbana, el plazo es el base de la prioridad")
    void plazoBasePorPrioridad() {
        cargaDeTrabajo(3, 0, 2);
        assertThat(tiempoEstimadoService.calcular(tipo(3), luminariaEn(true))).isEqualTo(24);
    }

    @Test
    @DisplayName("fuera del Área Urbana suma el traslado")
    void zonaRuralSumaTraslado() {
        cargaDeTrabajo(2, 0, 2);
        assertThat(tiempoEstimadoService.calcular(tipo(2), luminariaEn(false))).isEqualTo(72 + 24);
    }

    @Test
    @DisplayName("cada jornada completa de cola suma un día")
    void cargaDeCuadrillasSumaDias() {
        // 2 cuadrillas x 8 reclamos = 16 por día; 35 en cola = 2 jornadas completas
        cargaDeTrabajo(1, 35, 2);
        assertThat(tiempoEstimadoService.calcular(tipo(1), luminariaEn(true))).isEqualTo(168 + 48);
    }

    @Test
    @DisplayName("más cuadrillas activas reducen la demora por carga")
    void masCuadrillasMenosDemora() {
        cargaDeTrabajo(1, 35, 5);
        assertThat(tiempoEstimadoService.calcular(tipo(1), luminariaEn(true))).isEqualTo(168);
    }

    @Test
    @DisplayName("sin cuadrillas activas se toma capacidad de una")
    void sinCuadrillasTomaUna() {
        when(reclamoRepository.contarEnColaConPrioridadMinima(anyList(), eq(3))).thenReturn(8L);
        when(cuadrillaRepository.findByDeletedAtIsNull()).thenReturn(List.of());
        assertThat(tiempoEstimadoService.calcular(tipo(3), null)).isEqualTo(24 + 24);
    }

    @Test
    @DisplayName("sin prioridad no se estima plazo")
    void sinPrioridadDevuelveNull() {
        assertThat(tiempoEstimadoService.calcular(new TipoReclamo(), luminariaEn(true))).isNull();
    }
}
