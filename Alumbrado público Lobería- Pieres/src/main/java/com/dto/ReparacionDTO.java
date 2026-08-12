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

    public List<Long> getTecnicosIds() { return new ArrayList<>(tecnicosIds); }
    public List<MaterialConsumidoDTO> getMaterialesUsados() { return new ArrayList<>(materialesUsados); }
}
