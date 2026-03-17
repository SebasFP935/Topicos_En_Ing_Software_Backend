package com.upb.TSIS.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body de PATCH /api/operador/zonas/{id}/espacios/{eid}/posicion
 * Actualiza posición y/o tamaño de un espacio en el canvas.
 */
@Data
public class MoverEspacioRequest {

    @NotNull
    private Double x;

    @NotNull
    private Double y;

    /** Opcional — si null, mantiene el ancho actual. */
    private Double w;

    /** Opcional — si null, mantiene el alto actual. */
    private Double h;
}