package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.ListaEsperaRequest;
import com.upb.TSIS.entity.ListaEspera;

import java.util.List;

public interface IListaEsperaService {
    ListaEspera agregar(Integer usuarioId, ListaEsperaRequest request);
    List<ListaEspera> listarPorUsuario(Integer usuarioId);
    void cancelar(Integer listaEsperaId, Integer usuarioId);
    // Notifica al siguiente en espera cuando se libera un espacio en una zona
    void notificarSiguiente(Integer zonaId);
}