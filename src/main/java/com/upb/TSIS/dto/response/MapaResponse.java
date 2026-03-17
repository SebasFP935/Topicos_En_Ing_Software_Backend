package com.upb.TSIS.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Respuesta del endpoint GET /api/zonas/{id}/mapa
 *
 * Contiene todo lo necesario para que el frontend reconstruya
 * el mapa exactamente como el admin lo dejÃ³ configurado,
 * mÃ¡s el estado actual de cada espacio para la vista del usuario.
 */
@Data
@Builder
public class MapaResponse {

    private Integer zonaId;
    private String  zonaNombre;

    /** Dimensiones originales del canvas. */
    private Integer mapaAncho;
    private Integer mapaAlto;

    /** Imagen base del plano en Data URL (opcional). */
    private String planoImagen;

    /**
     * true si la zona ya tiene mapa configurado.
     * false si el admin aÃºn no ha dibujado nada.
     */
    private Boolean tieneMapa;

    /**
     * Elementos decorativos del plano (paredes y pasillos).
     * El frontend los usa para dibujar el fondo del mapa.
     */
    private List<PlanoElementoDto> plano;

    /**
     * Espacios de parqueo con su posiciÃ³n, tamaÃ±o y estado actual.
     * El estado refleja disponibilidad en tiempo real para la vista usuario.
     */
    private List<EspacioMapaDto> espacios;

    // â”€â”€ Inner DTOs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Data
    @Builder
    public static class PlanoElementoDto {
        private String type;  // "wall" | "road"
        private Double x, y, w, h;
    }

    @Data
    @Builder
    public static class EspacioMapaDto {
        private Integer id;
        private String  codigo;
        private String  tipoVehiculo;   // AUTO | MOTO | ELECTRICO | DISCAPACITADO
        private String  estado;         // DISPONIBLE | OCUPADO | RESERVADO | BLOQUEADO

        /** PosiciÃ³n y tamaÃ±o en el canvas. */
        private Double  x, y, w, h;

        /** Rotacion del espacio en grados. */
        private Double  angulo;
    }
}
