package com.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReparacionDTO {
    private Long reclamoId;
    private String observacion;
    private List<Long> tecnicosIds;
    private List<MaterialConsumidoDTO> materialesUsados;

    private List<Long> componentesDanadosIds;

    public List<Long> getTecnicosIds() {
        return tecnicosIds != null ? tecnicosIds : new ArrayList<>();
    }

    public List<MaterialConsumidoDTO> getMaterialesUsados() {
        return materialesUsados != null ? materialesUsados : new ArrayList<>();
    }

    public List<Long> getComponentesDanadosIds() {
        return componentesDanadosIds != null ? componentesDanadosIds : new ArrayList<>();
    }
}