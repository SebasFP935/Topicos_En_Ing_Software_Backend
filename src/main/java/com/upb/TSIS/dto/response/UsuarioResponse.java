package com.upb.TSIS.dto.response;

import com.upb.TSIS.entity.enums.RolUsuario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class UsuarioResponse {
    private Integer id;
    private String email;
    private String nombre;
    private String apellido;
    private String telefono;
    private RolUsuario rol;
    private String vehiculoPlaca;
    private String vehiculoModelo;
    private Boolean activo;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime creadoEn;
}