package com.upb.TSIS.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Body del endpoint PUT /api/zonas/{id}/mapa
 *
 * El frontend envÃ­a todo el estado del editor en una sola llamada:
 *   - Los elementos decorativos del plano (paredes y pasillos)
 *   - Los espacios de parqueo con su posiciÃ³n y tamaÃ±o en el canvas
 *   - Las dimensiones del canvas al momento de guardar
 */
@Data
public class MapaRequest {

    /** Ancho del canvas en pÃ­xeles al momento de guardar. */
    @NotNull
    private Integer mapaAncho;

    /** Alto del canvas en pÃ­xeles al momento de guardar. */
    @NotNull
    private Integer mapaAlto;

    /** Imagen base del plano en Data URL (opcional). */
    private String planoImagen;

    /**
     * Elementos decorativos: paredes y pasillos.
     * El frontend los dibuja para dar contexto visual al usuario,
     * pero no tienen lÃ³gica de negocio â€” solo se guardan y se devuelven.
     */
    @NotNull
    private List<PlanoElementoDto> plano;

    /**
     * Espacios de parqueo con su posiciÃ³n en el canvas.
     * Cada elemento referencia un espacio existente por su ID
     * o indica que debe crearse uno nuevo (id == null).
     */
    @NotNull
    @Valid
    private List<EspacioMapaDto> espacios;

    // â”€â”€ Inner DTOs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

        /** CÃ³digo Ãºnico del espacio (ej: "A-01"). Requerido si id == null. */
        private String codigo;

        /** Tipo de vehÃ­culo. Requerido si id == null. */
        private String tipoVehiculo;

        /** PosiciÃ³n X en el canvas (esquina superior izquierda). */
        @NotNull private Double x;

        /** PosiciÃ³n Y en el canvas. */
        @NotNull private Double y;

        /** Ancho del rectÃ¡ngulo en el canvas. */
        @NotNull private Double w;

        /** Alto del rectÃ¡ngulo en el canvas. */
        @NotNull private Double h;

        /** Rotacion del espacio en grados (0 = vertical normal). */
        private Double angulo;
    }
}
