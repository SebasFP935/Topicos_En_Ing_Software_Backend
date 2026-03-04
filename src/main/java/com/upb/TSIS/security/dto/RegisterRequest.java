package com.upb.TSIS.security.dto;

import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.entity.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    private TipoDocumento tipoDocumento;
    private String        numeroDocumento;
    private String        telefono;
    private String        vehiculoPlaca;
    private String        vehiculoModelo;

    // Si viene null se asigna USUARIO por defecto en el service
    // Solo el ADMIN puede registrar con otro rol
    private RolUsuario rol;
}