package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.ZonaRequest;
import com.upb.TSIS.entity.Zona;

import java.util.List;

public interface IZonaService {
    Zona crear(ZonaRequest request);
    Zona obtenerPorId(Integer id);
    List<Zona> listarPorSede(Integer sedeId);
    Zona actualizar(Integer id, ZonaRequest request);
    void desactivar(Integer id);
}
