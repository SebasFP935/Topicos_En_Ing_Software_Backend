package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.UsuarioRequest;
import com.upb.TSIS.dto.response.UsuarioResponse;
import com.upb.TSIS.entity.enums.RolUsuario;

import java.util.List;

public interface IUsuarioService {
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse obtenerPorId(Integer id);
    UsuarioResponse obtenerPorEmail(String email);
    List<UsuarioResponse> listarTodos();
    List<UsuarioResponse> listarPorRol(RolUsuario rol);
    UsuarioResponse actualizar(Integer id, UsuarioRequest request);
    void desactivar(Integer id);
}