package com.upb.TSIS.service.impl;

import com.upb.TSIS.entity.Notificacion;
import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.TipoNotificacion;
import com.upb.TSIS.repository.NotificacionRepository;
import com.upb.TSIS.service.INotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements INotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public void enviar(Usuario usuario, TipoNotificacion tipo, String asunto, String mensaje) {
        Notificacion notif = Notificacion.builder()
                .usuario(usuario)
                .tipo(tipo)
                .asunto(asunto)
                .mensaje(mensaje)
                .build();
        notificacionRepository.save(notif);
        // TODO: integrar proveedor de email/push (SendGrid, Firebase, etc.)
    }

    @Override
    @Transactional
    public void enviarConfirmacionReserva(Usuario usuario, Reserva reserva) {
        String asunto  = "Reserva confirmada - " + reserva.getEspacio().getCodigo();
        String mensaje = String.format(
                "Hola %s, tu reserva está confirmada.\nEspacio: %s\nFecha: %s\nHorario: %s - %s\nCódigo QR: %s",
                usuario.getNombre(),
                reserva.getEspacio().getCodigo(),
                reserva.getFechaReserva(),
                reserva.getFechaInicio().toLocalTime(),
                reserva.getFechaFin().toLocalTime(),
                reserva.getCodigoQr()
        );
        enviar(usuario, TipoNotificacion.EMAIL, asunto, mensaje);
    }

    @Override
    @Transactional
    public void enviarCancelacionReserva(Usuario usuario, Reserva reserva) {
        String asunto  = "Reserva cancelada - " + reserva.getEspacio().getCodigo();
        String mensaje = String.format(
                "Hola %s, tu reserva para el %s en el espacio %s ha sido cancelada.",
                usuario.getNombre(),
                reserva.getFechaReserva(),
                reserva.getEspacio().getCodigo()
        );
        enviar(usuario, TipoNotificacion.EMAIL, asunto, mensaje);
    }

    @Override
    @Transactional
    public void enviarRecordatorio(Usuario usuario, Reserva reserva) {
        String asunto  = "Recordatorio de reserva - " + reserva.getEspacio().getCodigo();
        String mensaje = String.format(
                "Hola %s, recuerda tu reserva de hoy a las %s en el espacio %s.",
                usuario.getNombre(),
                reserva.getFechaInicio().toLocalTime(),
                reserva.getEspacio().getCodigo()
        );
        enviar(usuario, TipoNotificacion.PUSH, asunto, mensaje);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notificacion> obtenerPorUsuario(Integer usuarioId) {
        return notificacionRepository.findByUsuario_IdOrderByFechaEnvioDesc(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(Integer usuarioId) {
        return notificacionRepository.countByUsuario_IdAndLeidaFalse(usuarioId);
    }

    @Override
    @Transactional
    public void marcarTodasLeidas(Integer usuarioId) {
        notificacionRepository.marcarTodasComoLeidas(usuarioId);
    }
}
