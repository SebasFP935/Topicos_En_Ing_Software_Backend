package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.SedeRequest;
import com.upb.TSIS.entity.Sede;

import java.util.List;

public interface ISedeService {
    Sede crear(SedeRequest request);
    Sede obtenerPorId(Integer id);
    List<Sede> listarActivas();
    Sede actualizar(Integer id, SedeRequest request);
    void desactivar(Integer id);
}