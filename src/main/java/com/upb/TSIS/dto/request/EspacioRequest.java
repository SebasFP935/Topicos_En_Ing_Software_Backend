package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.TipoVehiculo;
import lombok.Data;

@Data
public class EspacioRequest {
    private Integer zonaId;
    private String codigo;
    private TipoVehiculo tipoVehiculo;
    private Object coordenadas;
}