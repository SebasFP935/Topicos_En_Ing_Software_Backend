package com.upb.TSIS.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response del mapa para el módulo operador.
 * Extiende la información del mapa con:
 *  - imagenFondo: data URL base64 de la imagen del plano
 *  - codigoQr por espacio: UUID permanente del espacio físico
 */
@Data
@Builder
public class OperadorMapaResponse {

    private Integer zonaId;
    private String  zonaNombre;
    private Integer mapaAncho;
    private Integer mapaAlto;

    /** data URL (data:image/png;base64,...). Null si no se ha subido imagen. */
    private String  imagenFondo;

    private boolean tieneMapa;

    private List<PlanoElementoDto>   plano;
    private List<EspacioOperadorDto> espacios;

    // ── Inner DTOs ────────────────────────────────────────────────

    @Data
    @Builder
    public static class EspacioOperadorDto {
        private Integer id;
        private String  codigo;
        private String  tipoVehiculo;
        private String  estado;
        private Double  x;
        private Double  y;
        private Double  w;
        private Double  h;
        /** UUID permanente del espacio. Se codifica en la etiqueta QR física del slot. */
        private String  codigoQr;
    }

    @Data
    @Builder
    public static class PlanoElementoDto {
        private String type;
        private Double x;
        private Double y;
        private Double w;
        private Double h;
    }
}