package com.service;

import com.dto.LuminariaDetalleMapaDTO;
import com.dto.LuminariaMapaDTO;
import com.entity.Luminaria;
import com.entity.Reclamo;
import com.entity.TipoReclamo;
import com.entity.Zona;
import com.enums.ColorLuminaria;
import com.enums.EstadoReclamo;
import com.repository.LuminariaRepository;
import com.repository.ReclamoActivoView;
import com.repository.ReclamoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MapaLuminariaService: RF-05 (mapa de luminarias)")
class MapaLuminariaServiceTest {

    private static final GeometryFactory GF = new GeometryFactory();

    @Mock private LuminariaRepository luminariaRepository;
    @Mock private ReclamoRepository reclamoRepository;

    @InjectMocks private MapaLuminariaService mapaService;

    private static MapaLuminariaService.Activo activo(EstadoReclamo estado, Integer prioridad) {
        return new MapaLuminariaService.Activo(estado, prioridad);
    }

    private Luminaria luminaria(Long id, String estado, boolean areaUrbana) {
        Zona zona = new Zona();
        zona.setId(9L);
        zona.setNombre("Zona X");
        zona.setAreaUrbana(areaUrbana);
        Luminaria l = new Luminaria();
        l.setId(id);
        l.setEstado(estado);
        l.setTipo("LED");
        l.setPotencia("70W");
        l.setColumna("Hormigón");
        l.setZona(zona);
        l.setCoordenadas(GF.createPoint(new Coordinate(-58.78, -38.15)));
        return l;
    }

    private ReclamoActivoView view(Long luminariaId, EstadoReclamo estado, Integer prioridad) {
        return new ReclamoActivoView() {
            public Long getLuminariaId() { return luminariaId; }
            public EstadoReclamo getEstado() { return estado; }
            public Integer getPrioridad() { return prioridad; }
        };
    }

    @Test
    @DisplayName("verde: sin reclamos activos")
    void verdeSinReclamosActivos() {
        assertThat(MapaLuminariaService.calcularColor(List.of(), "Funciona")).isEqualTo(ColorLuminaria.VERDE);
        assertThat(MapaLuminariaService.calcularColor(List.of(), null)).isEqualTo(ColorLuminaria.VERDE);
    }

    @Test
    @DisplayName("verde: los reclamos resueltos, cerrados o rechazados no cuentan como activos")
    void verdeConReclamosInactivos() {
        assertThat(MapaLuminariaService.calcularColor(List.of(
                activo(EstadoReclamo.RESUELTO, 3),
                activo(EstadoReclamo.CERRADO, 3),
                activo(EstadoReclamo.RECHAZADO, 3)), "Funciona")).isEqualTo(ColorLuminaria.VERDE);
    }

    @Test
    @DisplayName("amarillo: reclamo activo de prioridad media o baja")
    void amarilloPrioridadMediaOBaja() {
        assertThat(MapaLuminariaService.calcularColor(List.of(activo(EstadoReclamo.PENDIENTE, 1)), "Funciona"))
                .isEqualTo(ColorLuminaria.AMARILLO);
        assertThat(MapaLuminariaService.calcularColor(List.of(activo(EstadoReclamo.ASIGNADO, 2)), "Funciona"))
                .isEqualTo(ColorLuminaria.AMARILLO);
    }

    @Test
    @DisplayName("rojo: reclamo activo de prioridad alta, aunque haya otros de menor prioridad")
    void rojoPrioridadAlta() {
        assertThat(MapaLuminariaService.calcularColor(List.of(
                activo(EstadoReclamo.PENDIENTE, 1),
                activo(EstadoReclamo.ASIGNADO, 3)), "Funciona")).isEqualTo(ColorLuminaria.ROJO);
    }

    @Test
    @DisplayName("rojo: sin servicio confirmado (espera de EDEA o luminaria fuera de servicio)")
    void rojoSinServicioConfirmado() {
        assertThat(MapaLuminariaService.calcularColor(List.of(activo(EstadoReclamo.ESPERA_EDEA, 1)), "Funciona"))
                .isEqualTo(ColorLuminaria.ROJO);
        assertThat(MapaLuminariaService.calcularColor(List.of(), "No funciona")).isEqualTo(ColorLuminaria.ROJO);
    }

    @Test
    @DisplayName("getMapa: agrupa los reclamos activos por luminaria y marca en gris las zonas no urbanas")
    void getMapaCalculaColorYMarcaGris() {
        Luminaria verde = luminaria(1L, "Funciona", true);
        Luminaria amarilla = luminaria(2L, "Funciona", true);
        Luminaria roja = luminaria(3L, "Funciona", true);
        Luminaria rural = luminaria(4L, "Funciona", false);
        Luminaria sinCoordenadas = luminaria(5L, "Funciona", true);
        sinCoordenadas.setCoordenadas(null);

        when(luminariaRepository.filtrar(null, null))
                .thenReturn(List.of(verde, amarilla, roja, rural, sinCoordenadas));
        when(reclamoRepository.findActivos(any())).thenReturn(List.of(
                view(2L, EstadoReclamo.PENDIENTE, 2),
                view(3L, EstadoReclamo.ASIGNADO, 3),
                view(3L, EstadoReclamo.PENDIENTE, 1)));

        List<LuminariaMapaDTO> mapa = mapaService.getMapa(null);

        assertThat(mapa).hasSize(4);
        assertThat(mapa.get(0).getColor()).isEqualTo(ColorLuminaria.VERDE);
        assertThat(mapa.get(0).isMarcaGris()).isFalse();
        assertThat(mapa.get(1).getColor()).isEqualTo(ColorLuminaria.AMARILLO);
        assertThat(mapa.get(2).getColor()).isEqualTo(ColorLuminaria.ROJO);
        // la marca gris es independiente del estado operativo
        assertThat(mapa.get(3).getColor()).isEqualTo(ColorLuminaria.VERDE);
        assertThat(mapa.get(3).isMarcaGris()).isTrue();
        assertThat(mapa.get(0).getLatitud()).isEqualTo(-38.15);
        assertThat(mapa.get(0).getLongitud()).isEqualTo(-58.78);
    }

    @Test
    @DisplayName("getDetalle: devuelve especificaciones técnicas y observaciones de los reclamos activos")
    void getDetalleIncluyeEspecificacionesYObservaciones() {
        Luminaria l = luminaria(7L, "Funciona", true);
        TipoReclamo tipo = new TipoReclamo();
        tipo.setNombre("Luminaria apagada");
        tipo.setPrioridad(2);
        Reclamo r = new Reclamo();
        r.setId(11L);
        r.setNumeroSeguimiento("REC-2026-00011");
        r.setEstado(EstadoReclamo.PENDIENTE);
        r.setTipoReclamo(tipo);
        r.setObservacion("Se apaga después de las 22");

        when(luminariaRepository.findById(7L)).thenReturn(Optional.of(l));
        when(reclamoRepository.findByLuminariaIdAndEstadoInOrderByFechaDesc(eq(7L), any())).thenReturn(List.of(r));

        LuminariaDetalleMapaDTO dto = mapaService.getDetalle(7L);

        assertThat(dto.getTecnologia()).isEqualTo("LED");
        assertThat(dto.getPotencia()).isEqualTo("70W");
        assertThat(dto.getColor()).isEqualTo(ColorLuminaria.AMARILLO);
        assertThat(dto.getObservacionesVecino()).hasSize(1);
        assertThat(dto.getObservacionesVecino().get(0).getObservacion()).isEqualTo("Se apaga después de las 22");
        assertThat(dto.getObservacionesVecino().get(0).getPrioridad()).isEqualTo(2);
    }

    @Test
    @DisplayName("getDetalle: falla si la luminaria no existe o está dada de baja")
    void getDetalleLuminariaInexistente() {
        when(luminariaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mapaService.getDetalle(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Luminaria no encontrada");
    }
}
