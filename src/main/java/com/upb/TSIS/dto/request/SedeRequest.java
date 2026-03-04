package com.upb.TSIS.dto.request;

import lombok.Data;

@Data
public class SedeRequest {
    private String nombre;
    private String direccion;
    private Double latitud;
    private Double longitud;
}