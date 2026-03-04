package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.entity.enums.TipoDocumento;
import lombok.Data;

@Data
public class UsuarioRequest {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String telefono;
    private RolUsuario rol;
    private String vehiculoPlaca;
    private String vehiculoModelo;
}