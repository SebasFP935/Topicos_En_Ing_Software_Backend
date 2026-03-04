package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.TipoZona;
import lombok.Data;

@Data
public class ZonaRequest {
    private Integer sedeId;
    private String nombre;
    private String descripcion;
    private TipoZona tipo;
}