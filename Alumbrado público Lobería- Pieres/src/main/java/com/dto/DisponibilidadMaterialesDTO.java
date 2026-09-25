package com.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// RF-18: disponibilidad de los repuestos que requiere el diagnóstico (solo Técnico/Administrador)
@Data
public class DisponibilidadMaterialesDTO {

    private boolean bloqueadoPorMaterial;
    private List<RepuestoInfo> repuestos = new ArrayList<>();

    @Data
    public static class RepuestoInfo {
        private Long componenteId;
        private String componente;
        private Long materialId;
        private String material;
        private boolean disponible;
    }
}
