package com.dto;

import lombok.Data;
import java.util.List;

@Data
public class HojaDeRutaReclamoBatchDTO {
    private Long hojaDeRutaId;
    private List<Long> reclamoIds;
}