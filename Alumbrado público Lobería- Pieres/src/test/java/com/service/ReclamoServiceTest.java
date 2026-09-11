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

        Reclamo resultado = reclamoService.saveFromDTO(dto);

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
}
