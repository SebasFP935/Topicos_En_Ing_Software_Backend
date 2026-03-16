// src/main/java/com/upb/TSIS/config/ParkingConstants.java
package com.upb.TSIS.config;

/**
 * Constantes de negocio del sistema de parking.
 * Modificar aquí para ajustar comportamiento sin tocar lógica.
 */
public final class ParkingConstants {

    private ParkingConstants() {}

    // ── Ventanas de escaneo QR ────────────────────────────────────────────────

    /** Minutos ANTES del inicio de la reserva en que se acepta el check-in */
    public static final int CHECKIN_VENTANA_ANTES_MIN = 5;

    /** Minutos DESPUÉS del inicio de la reserva en que aún se acepta el check-in */
    public static final int CHECKIN_VENTANA_DESPUES_MIN = 5;

    /** Minutos extras después del FIN de la reserva en que se acepta checkout sin penalización */
    public static final int CHECKOUT_VENTANA_EXTRA_MIN = 10;

    // ── Penalizaciones ────────────────────────────────────────────────────────

    /** Puntos descontados base por una penalización de checkout tardío */
    public static final int PUNTOS_BASE_PENALIZACION = 1;

    /**
     * Cada múltiplo de este número en penalizaciones ACTIVAS aumenta
     * en 1 los puntos descontados de la siguiente penalización.
     * Ejemplo con valor 5: 0-4 activas → 1 pto, 5-9 → 2 ptos, 10-14 → 3 ptos...
     */
    public static final int PENALIZACIONES_POR_INCREMENTO_PUNTO = 5;
}