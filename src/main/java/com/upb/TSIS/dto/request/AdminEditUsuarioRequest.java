// src/main/java/com/upb/TSIS/dto/request/AdminEditUsuarioRequest.java
package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.entity.enums.TipoDocumento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminEditUsuarioRequest {

    @Email(message = "Formato de email inválido")
    @Size(max = 150)
    private String email;

    @Size(max = 100, message = "Nombre demasiado largo")
    private String nombre;

    @Size(max = 100, message = "Apellido demasiado largo")
    private String apellido;

    private TipoDocumento tipoDocumento;

    @Size(max = 50)
    private String numeroDocumento;

    @Size(max = 30)
    private String telefono;

    @Size(max = 20)
    private String vehiculoPlaca;

    @Size(max = 100)
    private String vehiculoModelo;

    private Boolean activo;

    // Solo USUARIO u OPERADOR — jamás ADMIN
    private RolUsuario rol;

    @AssertTrue(message = "No se puede asignar el rol ADMIN desde este endpoint")
    public boolean isRolValido() {
        return rol == null || rol != RolUsuario.ADMIN;
    }
}