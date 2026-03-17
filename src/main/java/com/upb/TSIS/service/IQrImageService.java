package com.upb.TSIS.service;

/**
 * Genera imágenes QR como base64 PNG.
 * Usado tanto por el módulo operador (QR de espacios físicos)
 * como por ReservaTicketBuilder (QR de reservas).
 */
public interface IQrImageService {

    /**
     * Codifica 'contenido' en un QR PNG de 320x320 px y retorna base64 puro (sin data URL prefix).
     */
    String generarBase64(String contenido);

    /**
     * Igual que generarBase64 pero retorna el data URL completo: "data:image/png;base64,..."
     */
    String generarDataUrl(String contenido);
}