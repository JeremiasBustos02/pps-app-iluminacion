package com.service;

import com.dto.DisponibilidadMaterialesDTO;
import com.entity.Componente;
import com.entity.Material;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RF-18: indica si hay stock de los repuestos para los componentes diagnosticados como rotos (RF-13)
// y de los materiales que la cuadrilla necesita usar en la reparación
@Service
public class DisponibilidadMaterialService {

    public record MaterialRequerido(Material material, int cantidad) {
    }

    public DisponibilidadMaterialesDTO evaluar(List<Componente> componentesRotos) {
        return evaluar(componentesRotos, List.of());
    }

    public DisponibilidadMaterialesDTO evaluar(List<Componente> componentesRotos, List<MaterialRequerido> materialesUsados) {
        DisponibilidadMaterialesDTO dto = new DisponibilidadMaterialesDTO();

        // Unidades requeridas por material: cada componente roto consume una unidad de su repuesto.
        // Si el repuesto también figura en los materiales usados se toma la mayor de las dos cantidades
        // (es el mismo material, no se suma dos veces)
        Map<Long, Integer> porComponentes = new HashMap<>();
        for (Componente componente : componentesRotos) {
            if (componente.getRepuesto() != null) {
                porComponentes.merge(componente.getRepuesto().getId(), 1, Integer::sum);
            }
        }
        Map<Long, Integer> porUsados = new HashMap<>();
        for (MaterialRequerido usado : materialesUsados) {
            porUsados.merge(usado.material().getId(), usado.cantidad(), Integer::sum);
        }
        Map<Long, Integer> requeridos = new HashMap<>(porComponentes);
        porUsados.forEach((materialId, cantidad) -> requeridos.merge(materialId, cantidad, Math::max));

        for (Componente componente : componentesRotos) {
            if (componente.getRepuesto() == null) {
                continue; // no requiere repuesto de stock
            }
            agregar(dto, componente, componente.getRepuesto(), requeridos);
        }

        // Materiales usados que no son repuesto de ningún componente diagnosticado
        for (MaterialRequerido usado : materialesUsados) {
            Long materialId = usado.material().getId();
            boolean yaInformado = dto.getRepuestos().stream().anyMatch(r -> materialId.equals(r.getMaterialId()));
            if (!yaInformado) {
                agregar(dto, null, usado.material(), requeridos);
            }
        }
        return dto;
    }

    private void agregar(DisponibilidadMaterialesDTO dto, Componente componente, Material material,
                         Map<Long, Integer> requeridos) {
        DisponibilidadMaterialesDTO.RepuestoInfo info = new DisponibilidadMaterialesDTO.RepuestoInfo();
        if (componente != null) {
            info.setComponenteId(componente.getId());
            info.setComponente(componente.getNombre());
        }
        info.setMaterialId(material.getId());
        info.setMaterial(material.getNombre());
        info.setDisponible(hayStock(material, requeridos.get(material.getId())));
        dto.getRepuestos().add(info);

        if (!info.isDisponible()) {
            dto.setBloqueadoPorMaterial(true);
        }
    }

    private boolean hayStock(Material material, int requerido) {
        return material.getDeletedAt() == null
                && material.getCantidad() != null
                && material.getCantidad() >= requerido;
    }
}
