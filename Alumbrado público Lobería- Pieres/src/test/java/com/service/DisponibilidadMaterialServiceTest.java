package com.service;

import com.dto.DisponibilidadMaterialesDTO;
import com.entity.Componente;
import com.entity.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DisponibilidadMaterialService: RF-18 (indicador de disponibilidad de materiales)")
class DisponibilidadMaterialServiceTest {

    private final DisponibilidadMaterialService service = new DisponibilidadMaterialService();

    private Material material(long id, int cantidad) {
        Material material = new Material();
        material.setId(id);
        material.setNombre("Material " + id);
        material.setCantidad(cantidad);
        return material;
    }

    private Componente componente(long id, Material repuesto) {
        Componente componente = new Componente();
        componente.setId(id);
        componente.setNombre("Componente " + id);
        componente.setRepuesto(repuesto);
        return componente;
    }

    @Test
    @DisplayName("con stock suficiente no bloquea")
    void conStockNoBloquea() {
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(componente(1, material(10, 2))));
        assertThat(dto.isBloqueadoPorMaterial()).isFalse();
        assertThat(dto.getRepuestos()).singleElement().extracting("disponible").isEqualTo(true);
    }

    @Test
    @DisplayName("sin stock bloquea el reclamo")
    void sinStockBloquea() {
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(componente(1, material(10, 0))));
        assertThat(dto.isBloqueadoPorMaterial()).isTrue();
    }

    @Test
    @DisplayName("dos componentes con el mismo repuesto requieren dos unidades")
    void mismoRepuestoSumaUnidades() {
        Material lampara = material(10, 1);
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(componente(1, lampara), componente(2, lampara)));
        assertThat(dto.isBloqueadoPorMaterial()).isTrue();
    }

    @Test
    @DisplayName("un material dado de baja cuenta como sin stock")
    void materialDadoDeBajaSinStock() {
        Material material = material(10, 5);
        material.setDeletedAt(LocalDateTime.now());
        assertThat(service.evaluar(List.of(componente(1, material))).isBloqueadoPorMaterial()).isTrue();
    }

    @Test
    @DisplayName("componentes sin repuesto asociado no se evalúan")
    void componenteSinRepuestoNoBloquea() {
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(componente(1, null)));
        assertThat(dto.isBloqueadoPorMaterial()).isFalse();
        assertThat(dto.getRepuestos()).isEmpty();
    }

    @Test
    @DisplayName("materiales usados sin stock suficiente bloquean el reclamo")
    void materialUsadoSinStockBloquea() {
        Material cable = material(20, 3);
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(),
                List.of(new DisponibilidadMaterialService.MaterialRequerido(cable, 10)));
        assertThat(dto.isBloqueadoPorMaterial()).isTrue();
        assertThat(dto.getRepuestos()).singleElement().extracting("componente").isNull();
    }

    @Test
    @DisplayName("un repuesto que también figura como material usado no se cuenta dos veces")
    void repuestoYUsadoNoSeDuplican() {
        Material lampara = material(10, 1);
        DisponibilidadMaterialesDTO dto = service.evaluar(List.of(componente(1, lampara)),
                List.of(new DisponibilidadMaterialService.MaterialRequerido(lampara, 1)));
        assertThat(dto.isBloqueadoPorMaterial()).isFalse();
        assertThat(dto.getRepuestos()).hasSize(1);
    }
}
