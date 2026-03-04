package com.upb.TSIS.dto.request;

import com.upb.TSIS.entity.enums.TipoVehiculo;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaRequest {
    private Integer zonaId;
    private TipoVehiculo tipoVehiculo;
    private LocalDate fechaReserva;
    // Códigos de franja horaria según config_horarios de la entidad (ej: "A", "B")
    private String franjaInicio;
    private String franjaFin;
    // Espacio específico opcional; si es null se asigna automáticamente
    private Integer espacioId;
}