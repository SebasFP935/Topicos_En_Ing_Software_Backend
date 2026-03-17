package com.upb.TSIS.dto.request;

import lombok.Data;

/**
 * Body de POST /api/operador/zonas/{id}/generar-espacios
 * El sistema genera una grilla de espacios basándose en los parámetros indicados.
 * Todos los campos tienen valores por defecto, el operador puede ajustarlos.
 */
@Data
public class AutoLayoutRequest {

    /** Número de columnas de la grilla. Default: auto-calculado según ancho del canvas. */
    private Integer columnas;

    /** Número de filas de la grilla. Default: auto-calculado según alto del canvas. */
    private Integer filas;

    /** Tipo de vehículo para todos los espacios generados. */
    private String tipoVehiculo = "AUTO";

    /** Margen en px desde el borde izquierdo y derecho del canvas. */
    private double margenX = 30.0;

    /** Margen en px desde el borde superior e inferior del canvas. */
    private double margenY = 30.0;

    /** Espacio horizontal en px entre espacios. */
    private double espaciadoH = 12.0;

    /** Espacio vertical en px entre espacios. */
    private double espaciadoV = 12.0;

    /** Ancho de cada bloque de espacio en px. */
    private double espacioAncho = 52.0;

    /** Alto de cada bloque de espacio en px. */
    private double espacioAlto = 72.0;

    /**
     * Si true, elimina los espacios existentes sin reservas activas antes de generar.
     * Si false, añade los nuevos espacios a los existentes.
     */
    private boolean reemplazar = true;
}