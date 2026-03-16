// src/main/java/com/upb/TSIS/service/IQrTokenService.java
package com.upb.TSIS.service;

import com.upb.TSIS.dto.QrPayload;
import com.upb.TSIS.entity.Reserva;

public interface IQrTokenService {

    /**
     * Genera el token firmado HMAC-SHA256 a partir de la reserva.
     * Formato: {base64url(payload)}.{base64url(hmac)}
     */
    String generarToken(Reserva reserva);

    /**
     * Valida la firma del token y retorna el payload deserializado.
     * Lanza TokenQrInvalidoException si la firma es inválida o el token está malformado.
     */
    QrPayload validarToken(String token);

    /**
     * Construye la URL completa que irá codificada en el QR.
     * Ej: https://api.miapp.com/api/reservas/escanear/{token}
     */
    String generarUrlQr(Reserva reserva);
}