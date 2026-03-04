package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.BloqueoRequest;
import com.upb.TSIS.entity.BloqueoProgramado;

import java.util.List;

public interface IBloqueoProgramadoService {
    BloqueoProgramado crear(Integer adminId, BloqueoRequest request);
    List<BloqueoProgramado> listarPorEspacio(Integer espacioId);
    List<BloqueoProgramado> listarPorZona(Integer zonaId);
    void eliminar(Integer id);
}