// src/main/java/com/upb/TSIS/dto/QrPayload.java
package com.upb.TSIS.dto;

/**
 * Datos mínimos que viajan dentro del token QR firmado.
 * Se serializa a JSON compact antes de firmar.
 */
public record QrPayload(
        Integer r,   // reservaId
        Integer u,   // usuarioId
        Integer e,   // espacioId
        long    i,   // fechaInicio (epoch seconds)
        long    f    // fechaFin    (epoch seconds)
) {}