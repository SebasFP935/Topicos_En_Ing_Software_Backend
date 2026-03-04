package com.upb.TSIS.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BloqueoRequest {
    // Solo uno de los dos debe venir (espacio individual o zona completa)
    private Integer espacioId;
    private Integer zonaId;
    private String motivo;
    private LocalDateTime fechaInicioBloqueo;
    private LocalDateTime fechaFinBloqueo; // null = indefinido
}