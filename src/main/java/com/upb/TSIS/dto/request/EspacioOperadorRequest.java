package com.upb.TSIS.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body de POST /api/operador/zonas/{id}/espacios
 * Añade un único espacio de parqueo con posición y tipo.
 */
@Data
public class EspacioOperadorRequest {

    @NotBlank
    private String codigo;

    private String tipoVehiculo = "AUTO";

    @NotNull
    private Double x;

    @NotNull
    private Double y;

    private Double w = 52.0;
    private Double h = 72.0;
}