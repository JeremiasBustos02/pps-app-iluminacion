package com.service;

import com.dto.ReclamoDTO;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.entity.TipoReclamo;
import com.entity.Usuario;
import com.enums.EstadoReclamo;
import com.repository.LuminariaRepository;
import com.repository.ReclamoHistorialRepository;
import com.repository.ReclamoRepository;
import com.repository.TipoReclamoRepository;
import com.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReclamoService: RF-07 (creación de reclamo por tipificación)")
class ReclamoServiceTest {

    @Mock private ReclamoRepository reclamoRepository;
    @Mock private LuminariaRepository luminariaRepository;
    @Mock private TipoReclamoRepository tipoReclamoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReclamoHistorialRepository reclamoHistorialRepository;
    @Mock private TiempoEstimadoService tiempoEstimadoService;

    @InjectMocks private ReclamoService reclamoService;

    private ReclamoDTO dtoBase() {
        ReclamoDTO dto = new ReclamoDTO();
        dto.setLuminariaId(1L);
        dto.setTipoReclamoId(2L);
        dto.setUsuarioId(3L);
        return dto;
    }

    @Test
    @DisplayName("crea el reclamo con luminaria y tipo válidos, en estado PENDIENTE")
    void creaReclamoConDatosValidos() {
        ReclamoDTO dto = dtoBase();

        Luminaria luminaria = new Luminaria();
        luminaria.setId(1L);
        TipoReclamo tipoReclamo = new TipoReclamo();
        tipoReclamo.setId(2L);
        tipoReclamo.setNombre("Luminaria apagada");
        Usuario usuario = new Usuario();
        usuario.setId(3L);

        when(luminariaRepository.findById(1L)).thenReturn(Optional.of(luminaria));
        when(tipoReclamoRepository.findById(2L)).thenReturn(Optional.of(tipoReclamo));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(reclamoRepository.siguienteNumeroSeguimiento()).thenReturn(42L);
        when(reclamoRepository.save(org.mockito.ArgumentMatchers.any(Reclamo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(tiempoEstimadoService.calcular(tipoReclamo, luminaria)).thenReturn(96);

        Reclamo resultado = reclamoService.saveFromDTO(dto);

        // RF-10: el plazo informado sale del cálculo de tiempo estimado
        assertThat(resultado.getTiempoEstimado()).isEqualTo(96);
        assertThat(resultado.getFechaLimite()).isEqualTo(resultado.getFecha().plusHours(96));

        assertThat(resultado.getLuminaria()).isEqualTo(luminaria);
        assertThat(resultado.getTipoReclamo()).isEqualTo(tipoReclamo);
        assertThat(resultado.getUsuario()).isEqualTo(usuario);
        assertThat(resultado.getEstado()).isEqualTo(EstadoReclamo.PENDIENTE);
        assertThat(resultado.getNumeroSeguimiento())
                .isEqualTo("REC-" + java.time.Year.now().getValue() + "-00042");

        ArgumentCaptor<Reclamo> captor = ArgumentCaptor.forClass(Reclamo.class);
        verify(reclamoRepository).save(captor.capture());
        assertThat(captor.getValue().getFecha()).isNotNull();
    }

    @Test
    @DisplayName("RF-08: dos reclamos consecutivos reciben números de seguimiento distintos (correlativo real, no del reloj)")
    void generaNumerosDeSeguimientoUnicosPorSecuencia() {
        Luminaria luminaria = new Luminaria();
        luminaria.setId(1L);
        TipoReclamo tipoReclamo = new TipoReclamo();
        tipoReclamo.setId(2L);

        when(luminariaRepository.findById(1L)).thenReturn(Optional.of(luminaria));
        when(tipoReclamoRepository.findById(2L)).thenReturn(Optional.of(tipoReclamo));
        when(reclamoRepository.siguienteNumeroSeguimiento()).thenReturn(1L, 2L);
        when(reclamoRepository.save(org.mockito.ArgumentMatchers.any(Reclamo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReclamoDTO dto = new ReclamoDTO();
        dto.setLuminariaId(1L);
        dto.setTipoReclamoId(2L);

        Reclamo primero = reclamoService.saveFromDTO(dto);
        Reclamo segundo = reclamoService.saveFromDTO(dto);

        assertThat(primero.getNumeroSeguimiento()).isNotEqualTo(segundo.getNumeroSeguimiento());
    }

    @Test
    @DisplayName("rechaza el reclamo si no se seleccionó una luminaria en el mapa")
    void rechazaSinLuminaria() {
        ReclamoDTO dto = dtoBase();
        dto.setLuminariaId(null);

        assertThatThrownBy(() -> reclamoService.saveFromDTO(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("luminaria");

        verify(reclamoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("rechaza el reclamo si no se tipificó el problema")
    void rechazaSinTipoReclamo() {
        ReclamoDTO dto = dtoBase();
        dto.setTipoReclamoId(null);

        assertThatThrownBy(() -> reclamoService.saveFromDTO(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tipo de problema");

        verify(reclamoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("rechaza el reclamo si la luminaria seleccionada no existe")
    void rechazaLuminariaInexistente() {
        ReclamoDTO dto = dtoBase();
        when(luminariaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reclamoService.saveFromDTO(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Luminaria no encontrada");

        verify(reclamoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("rechaza el reclamo si el tipo de reclamo elegido no existe en el catálogo")
    void rechazaTipoReclamoInexistente() {
        ReclamoDTO dto = dtoBase();
        Luminaria luminaria = new Luminaria();
        luminaria.setId(1L);
        when(luminariaRepository.findById(1L)).thenReturn(Optional.of(luminaria));
        when(tipoReclamoRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reclamoService.saveFromDTO(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tipo de reclamo no encontrado");

        verify(reclamoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // --- RF-17: pausa del SLA en espera de EDEA ---

    private Reclamo reclamoEnEstado(EstadoReclamo estado) {
        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(estado);
        reclamo.setFecha(java.time.LocalDateTime.of(2026, 9, 1, 8, 0));
        reclamo.setTiempoEstimado(72);
        reclamo.setFechaLimite(java.time.LocalDateTime.of(2026, 9, 4, 8, 0));
        return reclamo;
    }

    @Test
    @DisplayName("RF-17: pasar a ESPERA_EDEA pausa el SLA")
    void pasarAEsperaEdeaPausaSla() {
        Reclamo reclamo = reclamoEnEstado(EstadoReclamo.ASIGNADO);
        when(reclamoRepository.findById(10L)).thenReturn(Optional.of(reclamo));
        when(reclamoRepository.save(org.mockito.ArgumentMatchers.any(Reclamo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Reclamo resultado = reclamoService.updateEstado(10L, EstadoReclamo.ESPERA_EDEA, "Corte de suministro");

        assertThat(resultado.getSlaPausadoDesde()).isNotNull();
        assertThat(resultado.getFechaLimite()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 4, 8, 0));
        assertThat(new com.dto.ReclamoResponseDTO(resultado).getFechaEstimadaResolucion()).isNull();
    }

    @Test
    @DisplayName("RF-17: al salir de ESPERA_EDEA se corre el plazo y se actualiza el tiempo estimado")
    void salirDeEsperaEdeaReanudaSla() {
        Reclamo reclamo = reclamoEnEstado(EstadoReclamo.ESPERA_EDEA);
        java.time.LocalDateTime inicioPausa = java.time.LocalDateTime.of(2026, 9, 2, 8, 0);
        reclamo.setSlaPausadoDesde(inicioPausa);

        reclamoService.reanudarSla(reclamo, inicioPausa.plusHours(48));

        assertThat(reclamo.getSlaPausadoDesde()).isNull();
        assertThat(reclamo.getMinutosPausa()).isEqualTo(48 * 60);
        assertThat(reclamo.getFechaLimite()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 6, 8, 0));
        assertThat(reclamo.getTiempoEstimado()).isEqualTo(72 + 48);
    }

    @Test
    @DisplayName("RF-17: volver a ASIGNADO desde ESPERA_EDEA registra el historial y reanuda el SLA")
    void updateEstadoDesdeEsperaEdeaReanuda() {
        Reclamo reclamo = reclamoEnEstado(EstadoReclamo.ESPERA_EDEA);
        reclamo.setSlaPausadoDesde(java.time.LocalDateTime.now().minusHours(2));
        when(reclamoRepository.findById(10L)).thenReturn(Optional.of(reclamo));
        when(reclamoRepository.save(org.mockito.ArgumentMatchers.any(Reclamo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Reclamo resultado = reclamoService.updateEstado(10L, EstadoReclamo.ASIGNADO, "Alta EDEA recibida");

        assertThat(resultado.getSlaPausadoDesde()).isNull();
        assertThat(resultado.getMinutosPausa()).isGreaterThanOrEqualTo(119);
        assertThat(resultado.getFechaLimite()).isAfter(java.time.LocalDateTime.of(2026, 9, 4, 9, 59));
        verify(reclamoHistorialRepository).save(org.mockito.ArgumentMatchers.any());
    }

    // --- RF-18: bloqueo por falta de material ---

    @Test
    @DisplayName("RF-18: pasar a ESPERA_MATERIAL corre el plazo por la reposición sin pausar el SLA")
    void esperaMaterialExtiendePlazo() {
        Reclamo reclamo = reclamoEnEstado(EstadoReclamo.ASIGNADO);
        when(reclamoRepository.findById(10L)).thenReturn(Optional.of(reclamo));
        when(reclamoRepository.save(org.mockito.ArgumentMatchers.any(Reclamo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Reclamo resultado = reclamoService.updateEstado(10L, EstadoReclamo.ESPERA_MATERIAL, "Falta balasto");

        assertThat(resultado.getSlaPausadoDesde()).isNull();
        assertThat(resultado.getFechaLimite()).isEqualTo(java.time.LocalDateTime.of(2026, 9, 7, 8, 0));
        assertThat(resultado.getTiempoEstimado()).isEqualTo(72 + TiempoEstimadoService.HORAS_REPOSICION_MATERIAL);
        assertThat(new com.dto.ReclamoResponseDTO(resultado).getEstado()).isEqualTo(EstadoReclamo.ESPERA_MATERIAL);
    }
}
