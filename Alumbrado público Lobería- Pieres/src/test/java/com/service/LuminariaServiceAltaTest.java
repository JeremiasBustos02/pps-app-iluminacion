package com.service;

import com.dto.LuminariaDTO;
import com.entity.Luminaria;
import com.entity.Zona;
import com.repository.LuminariaRepository;
import com.repository.ZonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LuminariaService: RF-06 (alta de nuevos puntos de luz)")
class LuminariaServiceAltaTest {

    @Mock private LuminariaRepository luminariaRepository;
    @Mock private ZonaRepository zonaRepository;

    @InjectMocks private LuminariaService luminariaService;

    private LuminariaDTO dtoValido() {
        LuminariaDTO dto = new LuminariaDTO();
        dto.setLatitud(-38.15);
        dto.setLongitud(-58.78);
        dto.setTipo("LED");
        dto.setZonaId(1L);
        return dto;
    }

    private void zonaExistente() {
        Zona zona = new Zona();
        zona.setId(1L);
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));
    }

    @Test
    @DisplayName("crea el punto con coordenadas, tipo y zona; el estado inicial es Funciona")
    void creaPuntoValido() {
        zonaExistente();
        when(luminariaRepository.save(any(Luminaria.class))).thenAnswer(i -> i.getArgument(0));

        luminariaService.save(dtoValido());

        ArgumentCaptor<Luminaria> captor = ArgumentCaptor.forClass(Luminaria.class);
        verify(luminariaRepository).save(captor.capture());
        Luminaria guardada = captor.getValue();
        assertThat(guardada.getTipo()).isEqualTo("LED");
        assertThat(guardada.getEstado()).isEqualTo("Funciona");
        assertThat(guardada.getZona().getId()).isEqualTo(1L);
        assertThat(guardada.getCoordenadas().getY()).isEqualTo(-38.15);
        assertThat(guardada.getCoordenadas().getX()).isEqualTo(-58.78);
    }

    @Test
    @DisplayName("normaliza el tipo Halógeno aunque llegue sin tilde o en minúsculas")
    void normalizaTipoHalogeno() {
        zonaExistente();
        when(luminariaRepository.save(any(Luminaria.class))).thenAnswer(i -> i.getArgument(0));
        LuminariaDTO dto = dtoValido();
        dto.setTipo("halogeno");

        Luminaria guardada = luminariaService.save(dto);

        assertThat(guardada.getTipo()).isEqualTo("Halógeno");
    }

    @Test
    @DisplayName("rechaza el alta sin coordenadas")
    void rechazaSinCoordenadas() {
        LuminariaDTO dto = dtoValido();
        dto.setLatitud(null);

        assertThatThrownBy(() -> luminariaService.save(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("coordenadas");
        verify(luminariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechaza coordenadas fuera de rango")
    void rechazaCoordenadasFueraDeRango() {
        LuminariaDTO dto = dtoValido();
        dto.setLatitud(95.0);

        assertThatThrownBy(() -> luminariaService.save(dto))
                .hasMessageContaining("fuera de rango");
    }

    @Test
    @DisplayName("rechaza el alta sin tipo o con un tipo distinto de LED/Halógeno")
    void rechazaTipoInvalido() {
        zonaExistente();
        LuminariaDTO sinTipo = dtoValido();
        sinTipo.setTipo(" ");
        assertThatThrownBy(() -> luminariaService.save(sinTipo)).hasMessageContaining("tipo");

        LuminariaDTO otro = dtoValido();
        otro.setTipo("Sodio");
        assertThatThrownBy(() -> luminariaService.save(otro)).hasMessageContaining("LED o Halógeno");
        verify(luminariaRepository, never()).save(any());
    }

    @Test
    @DisplayName("rechaza el alta sin zona, con zona inexistente o dada de baja")
    void rechazaZonaInvalida() {
        LuminariaDTO sinZona = dtoValido();
        sinZona.setZonaId(null);
        assertThatThrownBy(() -> luminariaService.save(sinZona)).hasMessageContaining("zona");

        when(zonaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> luminariaService.save(dtoValido())).hasMessageContaining("Zona no encontrada");

        Zona baja = new Zona();
        baja.setDeletedAt(LocalDateTime.now());
        when(zonaRepository.findById(1L)).thenReturn(Optional.of(baja));
        assertThatThrownBy(() -> luminariaService.save(dtoValido())).hasMessageContaining("Zona no encontrada");
        verify(luminariaRepository, never()).save(any());
    }
}
