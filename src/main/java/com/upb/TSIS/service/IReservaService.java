package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;

import java.util.List;

public interface IReservaService {
    ReservaResponse crear(Integer usuarioId, ReservaRequest request);
    ReservaResponse obtenerPorId(Integer id);
    ReservaResponse obtenerPorQr(String codigoQr);
    List<ReservaResponse> listarPorUsuario(Integer usuarioId);
    List<ReservaResponse> listarActualesDeHoy();
    ReservaResponse cancelar(Integer id, Integer usuarioId);
    ReservaResponse checkIn(String codigoQr, Integer operadorId);
    ReservaResponse checkOut(String codigoQr, Integer operadorId);
    void expirarReservasPasadas(); // llamado por scheduler
}