package com.upb.TSIS.dto.response;

import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EspacioResponse {
    private Integer id;
    private String codigo;
    private EstadoEspacio estado;
    private TipoVehiculo tipoVehiculo;
    private Object coordenadas;
    private Integer zonaId;
    private String zonaNombre;
}