package com.upb.TSIS.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body de POST /api/operador/zonas/{id}/imagen-fondo
 * El frontend envía la imagen como data URL base64 (incluye el prefijo data:image/...;base64,)
 * y opcionalmente las dimensiones reales de la imagen para ajustar el canvas.
 */
@Data
public class ImagenFondoRequest {

    /** data URL completa. Ej: "data:image/png;base64,iVBOR..." */
    @NotBlank
    private String imagenBase64;

    /** Ancho real de la imagen en px — usado para actualizar mapaAncho de la zona. Opcional. */
    private Integer imageWidth;

    /** Alto real de la imagen en px — usado para actualizar mapaAlto de la zona. Opcional. */
    private Integer imageHeight;
}