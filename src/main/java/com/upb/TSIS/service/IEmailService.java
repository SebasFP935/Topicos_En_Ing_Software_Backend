// src/main/java/com/upb/TSIS/service/IEmailService.java
package com.upb.TSIS.service;

import com.upb.TSIS.entity.Reserva;

public interface IEmailService {

    /**
     * Envía un correo de confirmación de reserva con el ticket adjunto.
     * El ticket es un HTML bonito con el código QR embebido.
     */
    void enviarTicketReserva(Reserva reserva, String qrUrl);

    /**
     * Envío genérico de correo plano (para otros tipos de notificación).
     */
    void enviarCorreoSimple(String destinatario, String asunto, String cuerpo);
}