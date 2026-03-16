// src/main/java/com/upb/TSIS/dto/response/ScanResponse.java
package com.upb.TSIS.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScanResponse {
    private String  accion;        // "CHECK_IN" | "CHECK_OUT"
    private String  mensaje;
    private String  estadoEspacio; // OCUPADO | DISPONIBLE
    private String  codigoEspacio;
    private String  zonaNombre;
    private LocalDateTime timestamp;
}