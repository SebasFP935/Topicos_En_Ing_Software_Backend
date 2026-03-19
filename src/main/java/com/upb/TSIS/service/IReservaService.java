package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.dto.response.ScanResponse;

import java.util.List;

public interface IReservaService {
    ReservaResponse crear(Integer usuarioId, ReservaRequest request);
    ReservaResponse obtenerPorId(Integer id);
    List<ReservaResponse> listarPorUsuario(Integer usuarioId);
    List<ReservaResponse> listarActualesDeHoy();
    ReservaResponse cancelar(Integer id, Integer usuarioId);
    void procesarReservasVencidas();      // scheduler: marca NO_SHOW y libera espacios
    void marcarEspaciosReservados();    // scheduler: activa RESERVADO cuando llega la franja
    ScanResponse escanear(String codigoQrFisicoEspacio, Integer usuarioId);
}
