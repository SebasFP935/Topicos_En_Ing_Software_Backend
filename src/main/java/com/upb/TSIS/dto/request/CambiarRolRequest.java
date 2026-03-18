// src/main/java/com/upb/TSIS/dto/request/CambiarRolRequest.java
package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.RolUsuario;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarRolRequest {

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @AssertTrue(message = "Solo se puede asignar USUARIO u OPERADOR")
    public boolean isRolPermitido() {
        return rol == RolUsuario.USUARIO || rol == RolUsuario.OPERADOR;
    }
}