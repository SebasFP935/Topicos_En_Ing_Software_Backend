package com.upb.TSIS.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListaEsperaRequest {
    private Integer zonaPreferidaId;
    private LocalDateTime fechaDeseadaInicio;
    private LocalDateTime fechaDeseadaFin;
}