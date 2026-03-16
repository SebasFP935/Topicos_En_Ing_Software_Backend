// src/main/java/com/upb/TSIS/service/impl/EmailServiceImpl.java
package com.upb.TSIS.service.impl;

import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.service.IEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender    mailSender;
    private final ReservaTicketBuilder ticketBuilder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${parking.mail.from-name}")
    private String fromName;

    // ── Ticket de reserva ─────────────────────────────────────────

    @Async
    @Override
    public void enviarTicketReserva(Reserva reserva, String qrUrl) {
        String destinatario = reserva.getUsuario().getEmail();
        String nombre       = reserva.getUsuario().getNombre();
        String espacio      = reserva.getEspacio().getCodigo();

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail, fromName);
            helper.setTo(destinatario);
            helper.setSubject("🅿 Tu reserva está confirmada — Espacio " + espacio);

            // Cuerpo del correo: texto plano simple
            String cuerpoEmail = """
                Hola %s,
                
                Tu reserva en el espacio %s ha sido confirmada.
                Encontrarás tu ticket de acceso adjunto a este correo.
                
                Recuerda presentar el código QR al ingresar y al salir del parqueo.
                
                — Sistema de Parqueo UPB
                """.formatted(nombre, espacio);

            helper.setText(cuerpoEmail, false);

            // Adjunto: el ticket HTML
            String ticketHtml = ticketBuilder.buildHtml(reserva, qrUrl);
            byte[] ticketBytes = ticketHtml.getBytes(StandardCharsets.UTF_8);

            helper.addAttachment(
                    "ticket-reserva-" + espacio + ".html",
                    () -> new java.io.ByteArrayInputStream(ticketBytes),
                    "text/html"
            );

            mailSender.send(mime);
            log.info("Ticket enviado a {} para reserva {}", destinatario, reserva.getId());

        } catch (Exception ex) {
            // No propagamos la excepción: el correo es secundario, la reserva ya fue guardada
            log.error("Error enviando ticket a {} para reserva {}: {}",
                    destinatario, reserva.getId(), ex.getMessage());
        }
    }

    // ── Correo simple ─────────────────────────────────────────────

    @Async
    @Override
    public void enviarCorreoSimple(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(destinatario);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
        } catch (Exception ex) {
            log.error("Error enviando correo simple a {}: {}", destinatario, ex.getMessage());
        }
    }
}