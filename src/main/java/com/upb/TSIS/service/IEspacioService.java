package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.EspacioRequest;
import com.upb.TSIS.dto.response.EspacioResponse;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;

import java.time.LocalDateTime;
import java.util.List;

public interface IEspacioService {
    EspacioResponse crear(EspacioRequest request);
    EspacioResponse obtenerPorId(Integer id);
    List<EspacioResponse> listarPorZona(Integer zonaId);
    List<EspacioResponse> listarDisponibles(Integer zonaId, TipoVehiculo tipoVehiculo,
                                            LocalDateTime inicio, LocalDateTime fin);
    EspacioResponse actualizarEstado(Integer id, EstadoEspacio nuevoEstado);
    void eliminar(Integer id);
    /** Devuelve la imagen PNG del QR físico como bytes crudos */
    byte[] obtenerQrPng(Integer id);
}