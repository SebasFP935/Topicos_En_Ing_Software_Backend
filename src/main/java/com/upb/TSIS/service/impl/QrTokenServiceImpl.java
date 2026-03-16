// src/main/java/com/upb/TSIS/service/impl/QrTokenServiceImpl.java
package com.upb.TSIS.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upb.TSIS.dto.QrPayload;
import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.exception.TokenQrInvalidoException;
import com.upb.TSIS.service.IQrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrTokenServiceImpl implements IQrTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    @Value("${parking.qr.secret}")
    private String secret;

    @Value("${parking.app.base-url}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Generación ────────────────────────────────────────────────

    @Override
    public String generarToken(Reserva reserva) {
        try {
            QrPayload payload = new QrPayload(
                    reserva.getId(),
                    reserva.getUsuario().getId(),
                    reserva.getEspacio().getId(),
                    reserva.getFechaInicio().toEpochSecond(java.time.ZoneOffset.UTC),
                    reserva.getFechaFin().toEpochSecond(java.time.ZoneOffset.UTC)
            );

            String payloadJson   = objectMapper.writeValueAsString(payload);
            String payloadB64    = B64_ENC.encodeToString(payloadJson.getBytes());
            String firma         = calcularHmac(payloadB64);

            return payloadB64 + "." + firma;

        } catch (Exception ex) {
            log.error("Error generando token QR para reserva {}", reserva.getId(), ex);
            throw new IllegalStateException("No se pudo generar el token QR", ex);
        }
    }

    @Override
    public String generarUrlQr(Reserva reserva) {
        return baseUrl + "/api/reservas/escanear/" + generarToken(reserva);
    }

    // ── Validación ────────────────────────────────────────────────

    @Override
    public QrPayload validarToken(String token) {
        if (token == null || !token.contains(".")) {
            throw new TokenQrInvalidoException("Formato de token inválido.");
        }

        int separador = token.lastIndexOf('.');
        String payloadB64 = token.substring(0, separador);
        String firmaToken = token.substring(separador + 1);

        // 1. Verificar firma
        String firmaEsperada = calcularHmac(payloadB64);
        if (!firmaEsperada.equals(firmaToken)) {
            throw new TokenQrInvalidoException("Firma QR inválida. Posible falsificación.");
        }

        // 2. Deserializar payload
        try {
            String payloadJson = new String(B64_DEC.decode(payloadB64));
            return objectMapper.readValue(payloadJson, QrPayload.class);
        } catch (Exception ex) {
            throw new TokenQrInvalidoException("No se pudo leer el contenido del QR.");
        }
    }

    // ── Helper HMAC ───────────────────────────────────────────────

    private String calcularHmac(String datos) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(), HMAC_ALGO));
            byte[] firma = mac.doFinal(datos.getBytes());
            return B64_ENC.encodeToString(firma);
        } catch (Exception ex) {
            throw new IllegalStateException("Error calculando HMAC", ex);
        }
    }
}