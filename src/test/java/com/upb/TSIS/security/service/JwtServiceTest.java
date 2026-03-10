package com.upb.TSIS.security.service;

import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "5b04a5e3d2f1a9c8b7e6d5c4f3a2b1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2";

    @Test
    void shouldInvalidateTokenWhenTokenVersionChanges() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900000L);

        Usuario usuario = Usuario.builder()
                .id(7)
                .email("usuario@test.com")
                .passwordHash("hash")
                .nombre("Ana")
                .apellido("Perez")
                .rol(RolUsuario.USUARIO)
                .tokenVersion(1)
                .build();

        String token = jwtService.generarAccessToken(usuario);

        assertTrue(jwtService.esValido(token, usuario));

        usuario.setTokenVersion(2);

        assertFalse(jwtService.esValido(token, usuario));
    }
}
