package com.upb.TSIS.service;

import com.upb.TSIS.entity.Notificacion;
import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.TipoNotificacion;

import java.util.List;

public interface INotificacionService {
    void enviar(Usuario usuario, TipoNotificacion tipo, String asunto, String mensaje);
    void enviarConfirmacionReserva(Usuario usuario, Reserva reserva);
    void enviarCancelacionReserva(Usuario usuario, Reserva reserva);
    void enviarRecordatorio(Usuario usuario, Reserva reserva);
    List<Notificacion> obtenerPorUsuario(Integer usuarioId);
    long contarNoLeidas(Integer usuarioId);
    void marcarTodasLeidas(Integer usuarioId);
}