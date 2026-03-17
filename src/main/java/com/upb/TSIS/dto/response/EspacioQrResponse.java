package com.upb.TSIS.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EspacioQrResponse {
    private Integer id;
    private String codigo;
    private String codigoQr;
}
