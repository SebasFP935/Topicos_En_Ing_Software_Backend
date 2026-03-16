// src/main/java/com/upb/TSIS/exception/TokenQrInvalidoException.java
package com.upb.TSIS.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenQrInvalidoException extends RuntimeException {
    public TokenQrInvalidoException(String mensaje) {
        super(mensaje);
    }
}