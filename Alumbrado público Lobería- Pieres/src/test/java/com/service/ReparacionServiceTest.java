package com.service;

import com.dto.MaterialConsumidoDTO;
import com.dto.ReparacionDTO;
import com.dto.ReparacionResponseDTO;
import com.entity.Componente;
import com.entity.Material;
import com.entity.Reclamo;
import com.entity.Reparacion;
import com.enums.EstadoReclamo;
import com.entity.ReparacionMaterial;
import com.repository.ComponenteRepository;
import com.repository.MaterialRepository;
import com.repository.ReparacionComponenteRepository;
import com.repository.ReparacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReparacionService: resolución del reclamo al registrar la reparación")
class ReparacionServiceTest {

    @Mock private ReparacionRepository reparacionRepository;
    @Mock private ReclamoService reclamoService;
    @Mock private ComponenteRepository componenteRepository;
    @Mock private ReparacionComponenteRepository reparacionComponenteRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private ReparacionMaterialService reparacionMaterialService;
    @Spy private DisponibilidadMaterialService disponibilidadMaterialService;

    @InjectMocks private ReparacionService reparacionService;

    private ReparacionDTO dtoConReclamo() {
        ReparacionDTO dto = new ReparacionDTO();
        dto.setReclamoId(10L);
        dto.setObservacion("Se cambió la fotocélula");
        return dto;
    }

    @Test
    @DisplayName("resuelve el reclamo pasando por updateEstado (valida transición y registra historial)")
    void resuelveReclamoConUpdateEstado() {
        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(EstadoReclamo.RESUELTO);
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString())).thenReturn(reclamo);
        when(reparacionRepository.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReparacionResponseDTO resultado = reparacionService.registrarReparacion(dtoConReclamo());

        assertThat(resultado.getReclamoId()).isEqualTo(10L);
        assertThat(resultado.getEstadoReclamo()).isEqualTo(EstadoReclamo.RESUELTO);
        verify(reclamoService).updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString());
    }

    @Test
    @DisplayName("RF-17: no registra la reparación si el reclamo sigue en espera de EDEA")
    void noResuelveReclamoEnEsperaEdea() {
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString()))
                .thenThrow(new RuntimeException("No se puede pasar de ESPERA_EDEA a RESUELTO"));

        assertThatThrownBy(() -> reparacionService.registrarReparacion(dtoConReclamo()))
                .hasMessageContaining("ESPERA_EDEA");

        verify(reparacionRepository, never()).save(any());
    }

    // --- RF-18: disponibilidad de repuestos al diagnosticar ---

    private Componente componenteConRepuesto(long id, String nombre, int stock) {
        Material material = new Material();
        material.setId(id * 100);
        material.setNombre("Repuesto " + nombre);
        material.setCantidad(stock);
        Componente componente = new Componente();
        componente.setId(id);
        componente.setNombre(nombre);
        componente.setRepuesto(material);
        return componente;
    }

    @Test
    @DisplayName("RF-18: si falta un repuesto el reclamo queda en ESPERA_MATERIAL en vez de resolverse")
    void faltaDeRepuestoBloqueaReclamo() {
        ReparacionDTO dto = dtoConReclamo();
        dto.setComponentesDanadosIds(List.of(1L, 2L));
        when(componenteRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(componenteConRepuesto(1L, "Fotocontrol", 5)));
        when(componenteRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(componenteConRepuesto(2L, "Balasto", 0)));

        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(EstadoReclamo.ESPERA_MATERIAL);
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.ESPERA_MATERIAL), anyString())).thenReturn(reclamo);
        when(reparacionRepository.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReparacionResponseDTO resultado = reparacionService.registrarReparacion(dto);

        verify(reclamoService).updateEstado(eq(10L), eq(EstadoReclamo.ESPERA_MATERIAL),
                org.mockito.ArgumentMatchers.contains("Repuesto Balasto"));
        verify(reclamoService, never()).updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString());
        assertThat(resultado.getEstadoReclamo()).isEqualTo(EstadoReclamo.ESPERA_MATERIAL);
        assertThat(resultado.getDisponibilidadMateriales().isBloqueadoPorMaterial()).isTrue();
        assertThat(resultado.getDisponibilidadMateriales().getRepuestos())
                .extracting(r -> r.getComponente() + "=" + r.isDisponible())
                .containsExactly("Fotocontrol=true", "Balasto=false");
    }

    @Test
    @DisplayName("RF-18: con todos los repuestos en stock el reclamo se resuelve")
    void conRepuestosDisponiblesResuelve() {
        ReparacionDTO dto = dtoConReclamo();
        dto.setComponentesDanadosIds(List.of(1L));
        when(componenteRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(componenteConRepuesto(1L, "Lámpara", 3)));
        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(EstadoReclamo.RESUELTO);
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString())).thenReturn(reclamo);
        when(reparacionRepository.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReparacionResponseDTO resultado = reparacionService.registrarReparacion(dto);

        assertThat(resultado.getDisponibilidadMateriales().isBloqueadoPorMaterial()).isFalse();
        assertThat(resultado.getEstadoReclamo()).isEqualTo(EstadoReclamo.RESUELTO);
    }

    private MaterialConsumidoDTO usado(long materialId, int cantidad) {
        MaterialConsumidoDTO m = new MaterialConsumidoDTO();
        m.setMaterialId(materialId);
        m.setCantidad(cantidad);
        return m;
    }

    private Material materialEnStock(long id, String nombre, int stock) {
        Material material = new Material();
        material.setId(id);
        material.setNombre(nombre);
        material.setCantidad(stock);
        when(materialRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(material));
        return material;
    }

    @Test
    @DisplayName("RF-18: si un material usado no alcanza, el reclamo queda en ESPERA_MATERIAL y solo se descuenta lo disponible")
    void materialUsadoInsuficienteBloqueaReclamo() {
        ReparacionDTO dto = dtoConReclamo();
        dto.setMaterialesUsados(List.of(usado(1L, 2), usado(2L, 10)));
        Material cinta = materialEnStock(1L, "Cinta aisladora", 5);
        materialEnStock(2L, "Cable 2x1.5", 3);

        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(EstadoReclamo.ESPERA_MATERIAL);
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.ESPERA_MATERIAL), anyString())).thenReturn(reclamo);
        when(reparacionRepository.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ReparacionResponseDTO resultado = reparacionService.registrarReparacion(dto);

        assertThat(resultado.getEstadoReclamo()).isEqualTo(EstadoReclamo.ESPERA_MATERIAL);
        verify(reclamoService).updateEstado(eq(10L), eq(EstadoReclamo.ESPERA_MATERIAL),
                org.mockito.ArgumentMatchers.contains("Cable 2x1.5"));

        org.mockito.ArgumentCaptor<ReparacionMaterial> captor = org.mockito.ArgumentCaptor.forClass(ReparacionMaterial.class);
        verify(reparacionMaterialService).addMaterial(captor.capture());
        assertThat(captor.getValue().getMaterial()).isEqualTo(cinta);
        assertThat(captor.getValue().getCantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("RF-18: materiales usados con stock suficiente se descuentan y el reclamo se resuelve")
    void materialesUsadosDisponiblesResuelve() {
        ReparacionDTO dto = dtoConReclamo();
        dto.setMaterialesUsados(List.of(usado(1L, 2)));
        materialEnStock(1L, "Cinta aisladora", 5);
        Reclamo reclamo = new Reclamo();
        reclamo.setId(10L);
        reclamo.setEstado(EstadoReclamo.RESUELTO);
        when(reclamoService.updateEstado(eq(10L), eq(EstadoReclamo.RESUELTO), anyString())).thenReturn(reclamo);
        when(reparacionRepository.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        reparacionService.registrarReparacion(dto);

        verify(reparacionMaterialService).addMaterial(any(ReparacionMaterial.class));
    }

    @Test
    @DisplayName("una cantidad inválida de material se rechaza antes de tocar el reclamo")
    void cantidadInvalidaSeRechaza() {
        ReparacionDTO dto = dtoConReclamo();
        dto.setMaterialesUsados(List.of(usado(1L, 0)));

        assertThatThrownBy(() -> reparacionService.registrarReparacion(dto))
                .hasMessageContaining("mayor a cero");
        verify(reclamoService, never()).updateEstado(any(), any(), any());
    }
}
