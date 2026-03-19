package com.upb.TSIS.dto.response;

import com.upb.TSIS.entity.enums.EstadoReserva;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservaResponse {
    private Integer id;
    private LocalDate fechaReserva;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoReserva estado;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    // Info resumida de lo que referencia
    private String codigoEspacio;
    private String codigoQrFisico;
    private String zonaNombre;
    private String sedeNombre;
    private String usuarioNombre;
    private String usuarioEmail;
    private LocalDateTime creadoEn;
}
