package com.upb.TSIS.service;

public interface IQrImageService {
    /** Devuelve imagen PNG del QR codificado como Base64 */
    String generarBase64(String contenido);
}