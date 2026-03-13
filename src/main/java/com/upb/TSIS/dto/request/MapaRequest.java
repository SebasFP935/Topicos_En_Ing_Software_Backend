package com.upb.TSIS.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Body del endpoint PUT /api/zonas/{id}/mapa
 *
 * El frontend envía todo el estado del editor en una sola llamada:
 *   - Los elementos decorativos del plano (paredes y pasillos)
 *   - Los espacios de parqueo con su posición y tamaño en el canvas
 *   - Las dimensiones del canvas al momento de guardar
 */
@Data
public class MapaRequest {

    /** Ancho del canvas en píxeles al momento de guardar. */
    @NotNull
    private Integer mapaAncho;

    /** Alto del canvas en píxeles al momento de guardar. */
    @NotNull
    private Integer mapaAlto;

    /**
     * Elementos decorativos: paredes y pasillos.
     * El frontend los dibuja para dar contexto visual al usuario,
     * pero no tienen lógica de negocio — solo se guardan y se devuelven.
     */
    @NotNull
    private List<PlanoElementoDto> plano;

    /**
     * Espacios de parqueo con su posición en el canvas.
     * Cada elemento referencia un espacio existente por su ID
     * o indica que debe crearse uno nuevo (id == null).
     */
    @NotNull
    @Valid
    private List<EspacioMapaDto> espacios;

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data
    public static class PlanoElementoDto {
        /** "wall" o "road" */
        @NotNull
        private String type;

        @NotNull private Double x;
        @NotNull private Double y;
        @NotNull private Double w;
        @NotNull private Double h;
    }

    @Data
    public static class EspacioMapaDto {
        /**
         * ID del espacio existente en la BD.
         * Null si es un espacio nuevo que debe crearse.
         */
        private Integer id;

        /** Código único del espacio (ej: "A-01"). Requerido si id == null. */
        private String codigo;

        /** Tipo de vehículo. Requerido si id == null. */
        private String tipoVehiculo;

        /** Posición X en el canvas (esquina superior izquierda). */
        @NotNull private Double x;

        /** Posición Y en el canvas. */
        @NotNull private Double y;

        /** Ancho del rectángulo en el canvas. */
        @NotNull private Double w;

        /** Alto del rectángulo en el canvas. */
        @NotNull private Double h;
    }
}