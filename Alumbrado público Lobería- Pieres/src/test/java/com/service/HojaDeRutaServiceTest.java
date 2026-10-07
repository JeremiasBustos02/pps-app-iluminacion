package com.service;

import com.dto.HojaDelDiaDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.entity.Cuadrilla;
import com.entity.CuadrillaTecnico;
import com.entity.HojaDeRuta;
import com.entity.HojaDeRutaReclamo;
import com.entity.Reclamo;
import com.entity.TipoReclamo;
import com.enums.EstadoReclamo;
import com.repository.CuadrillaTecnicoRepository;
import com.repository.HojaDeRutaReclamoRepository;
import com.repository.HojaDeRutaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HojaDeRutaService: RF-20 (hoja de ruta del día y atención de reclamos)")
class HojaDeRutaServiceTest {

    private static final Long TECNICO_ID = 7L;
    private static final Long HOJA_ID = 3L;

    @Mock private HojaDeRutaRepository hojaDeRutaRepository;
    @Mock private CuadrillaTecnicoRepository cuadrillaTecnicoRepository;
    @Mock private HojaDeRutaReclamoRepository hojaDeRutaReclamoRepository;
    @Mock private ReclamoService reclamoService;
    @Mock private ReparacionService reparacionService;

    @InjectMocks private HojaDeRutaService hojaDeRutaService;

    private HojaDeRuta hoja;

    @BeforeEach
    void cuadrillaConHojaDelDia() {
        Cuadrilla cuadrilla = new Cuadrilla();
        cuadrilla.setId(1L);
        cuadrilla.setNombre("Cuadrilla Norte");
        CuadrillaTecnico ct = new CuadrillaTecnico();
        ct.setCuadrilla(cuadrilla);
        when(cuadrillaTecnicoRepository.findByUsuarioId(TECNICO_ID)).thenReturn(List.of(ct));

        hoja = new HojaDeRuta();
        hoja.setId(HOJA_ID);
        hoja.setCuadrilla(cuadrilla);
        hoja.setFecha(LocalDateTime.now());
        when(hojaDeRutaRepository.findByCuadrillaIdAndFechaBetween(eq(1L), any(), any())).thenReturn(List.of(hoja));
    }

    private Reclamo reclamo(long id, EstadoReclamo estado, int prioridad) {
        TipoReclamo tipo = new TipoReclamo();
        tipo.setNombre("Tipo " + prioridad);
        tipo.setPrioridad(prioridad);
        Reclamo reclamo = new Reclamo();
        reclamo.setId(id);
        reclamo.setEstado(estado);
        reclamo.setTipoReclamo(tipo);
        reclamo.setFecha(LocalDateTime.now());
        return reclamo;
    }

    private HojaDeRutaReclamo enHoja(Reclamo reclamo) {
        HojaDeRutaReclamo hdr = new HojaDeRutaReclamo();
        hdr.setHojaDeRuta(hoja);
        hdr.setReclamo(reclamo);
        return hdr;
    }

    private ReparacionDTO diagnostico(String observacion) {
        ReparacionDTO dto = new ReparacionDTO();
        dto.setObservacion(observacion);
        return dto;
    }

    @Test
    @DisplayName("la hoja del día incluye sus reclamos ordenados por prioridad")
    void hojaDelDiaConReclamosPorPrioridad() {
        when(hojaDeRutaReclamoRepository.findByHojaDeRutaId(HOJA_ID)).thenReturn(List.of(
                enHoja(reclamo(1L, EstadoReclamo.ASIGNADO, 1)),
                enHoja(reclamo(2L, EstadoReclamo.ASIGNADO, 3)),
                enHoja(reclamo(3L, EstadoReclamo.RESUELTO, 2))));

        List<HojaDelDiaDTO> hojas = hojaDeRutaService.getMiHojaDelDia(TECNICO_ID);

        assertThat(hojas).singleElement().satisfies(h -> {
            assertThat(h.getCuadrilla()).isEqualTo("Cuadrilla Norte");
            assertThat(h.getReclamos()).extracting(HojaDelDiaDTO.ReclamoEnHojaInfo::getId).containsExactly(2L, 3L, 1L);
            assertThat(h.getReclamos()).extracting(HojaDelDiaDTO.ReclamoEnHojaInfo::isPendienteDeAtencion)
                    .containsExactly(true, false, true);
        });
    }

    @Test
    @DisplayName("atender un reclamo registra la reparación con el técnico autenticado")
    void atenderReclamoRegistraReparacion() {
        when(hojaDeRutaReclamoRepository.findByHojaDeRutaIdAndReclamoId(HOJA_ID, 10L))
                .thenReturn(Optional.of(new HojaDeRutaReclamo()));
        when(reclamoService.findById(10L)).thenReturn(Optional.of(reclamo(10L, EstadoReclamo.ASIGNADO, 2)));
        when(reparacionService.registrarReparacion(any())).thenReturn(null);

        ReparacionDTO dto = diagnostico("Se reemplazó el fotocontrol");
        dto.setComponentesDanadosIds(List.of(4L));
        hojaDeRutaService.atenderReclamo(TECNICO_ID, 10L, dto);

        ArgumentCaptor<ReparacionDTO> captor = ArgumentCaptor.forClass(ReparacionDTO.class);
        verify(reparacionService).registrarReparacion(captor.capture());
        assertThat(captor.getValue().getReclamoId()).isEqualTo(10L);
        assertThat(captor.getValue().getTecnicosIds()).containsExactly(TECNICO_ID);
        assertThat(captor.getValue().getComponentesDanadosIds()).containsExactly(4L);
        verify(reclamoService, never()).updateEstado(anyLong(), any(), anyString());
    }

    @Test
    @DisplayName("un reclamo todavía pendiente pasa a asignado antes de registrar la reparación")
    void reclamoPendientePasaAAsignado() {
        when(hojaDeRutaReclamoRepository.findByHojaDeRutaIdAndReclamoId(HOJA_ID, 10L))
                .thenReturn(Optional.of(new HojaDeRutaReclamo()));
        when(reclamoService.findById(10L)).thenReturn(Optional.of(reclamo(10L, EstadoReclamo.PENDIENTE, 2)));
        when(reparacionService.registrarReparacion(any())).thenReturn(new ReparacionResponseDTO(new com.entity.Reparacion()));

        hojaDeRutaService.atenderReclamo(TECNICO_ID, 10L, diagnostico("Cambio de lámpara"));

        verify(reclamoService).updateEstado(eq(10L), eq(EstadoReclamo.ASIGNADO), anyString());
        verify(reparacionService).registrarReparacion(any());
    }

    @Test
    @DisplayName("no permite atender reclamos que no están en la hoja del día de su cuadrilla")
    void reclamoFueraDeLaHojaSeRechaza() {
        when(hojaDeRutaReclamoRepository.findByHojaDeRutaIdAndReclamoId(HOJA_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hojaDeRutaService.atenderReclamo(TECNICO_ID, 99L, diagnostico("Arreglado")))
                .hasMessageContaining("hoja de ruta del día");
        verify(reparacionService, never()).registrarReparacion(any());
    }

    @Test
    @DisplayName("RF-14: las observaciones del trabajo son obligatorias")
    void observacionObligatoria() {
        assertThatThrownBy(() -> hojaDeRutaService.atenderReclamo(TECNICO_ID, 10L, diagnostico("  ")))
                .hasMessageContaining("observaciones");
        verify(reparacionService, never()).registrarReparacion(any());
    }
}
