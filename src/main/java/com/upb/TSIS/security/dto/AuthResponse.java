package com.upb.TSIS.security.dto;

import com.upb.TSIS.entity.enums.RolUsuario;
import lombok.Getter;

@Getter
public class AuthResponse {

    private final String     tipo         = "Bearer";
    private final String     accessToken;
    private final String     refreshToken;
    private final Integer    usuarioId;
    private final String     email;
    private final String     nombre;
    private final String     apellido;
    private final RolUsuario rol;

    public AuthResponse(String accessToken, String refreshToken,
                        Integer usuarioId, String email,
                        String nombre, String apellido, RolUsuario rol) {
        this.accessToken  = accessToken;
        this.refreshToken = refreshToken;
        this.usuarioId    = usuarioId;
        this.email        = email;
        this.nombre       = nombre;
        this.apellido     = apellido;
        this.rol          = rol;
    }
}