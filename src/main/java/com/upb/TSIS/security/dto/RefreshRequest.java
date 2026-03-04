package com.upb.TSIS.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}