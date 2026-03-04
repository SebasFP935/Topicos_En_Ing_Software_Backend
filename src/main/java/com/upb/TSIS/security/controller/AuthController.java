package com.upb.TSIS.security.controller;

import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.security.dto.AuthResponse;
import com.upb.TSIS.security.dto.LoginRequest;
import com.upb.TSIS.security.dto.RefreshRequest;
import com.upb.TSIS.security.dto.RegisterRequest;
import com.upb.TSIS.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/register
    // Registro libre para rol USUARIO; para otros roles requiere ADMIN
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // POST /api/auth/logout  (requiere estar autenticado)
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal Usuario usuario) {
        authService.logout(usuario.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente."));
    }

    // GET /api/auth/me  — datos del usuario autenticado actual
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(Map.of(
                "id",       usuario.getId(),
                "email",    usuario.getEmail(),
                "nombre",   usuario.getNombre(),
                "apellido", usuario.getApellido(),
                "rol",      usuario.getRol()
        ));
    }
}