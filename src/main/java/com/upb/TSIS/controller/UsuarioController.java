package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.AdminEditUsuarioRequest;
import com.upb.TSIS.dto.request.CambiarRolRequest;
import com.upb.TSIS.dto.request.UsuarioRequest;
import com.upb.TSIS.dto.response.UsuarioResponse;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.service.IUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService usuarioService;

    // GET /api/usuarios
    // Solo ADMIN puede ver todos los usuarios
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // GET /api/usuarios/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    // GET /api/usuarios/rol/{rol}
    @GetMapping("/rol/{rol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarPorRol(@PathVariable RolUsuario rol) {
        return ResponseEntity.ok(usuarioService.listarPorRol(rol));
    }

    // POST /api/usuarios
    // Creación manual por admin (distinto a /auth/register)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    // PUT /api/usuarios/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    // DELETE /api/usuarios/{id}  → desactiva, no borra físicamente
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/usuarios/gestion
// Lista todos los no-admins (USUARIO + OPERADOR) para el dashboard
    @GetMapping("/gestion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> listarParaDashboard(
            @RequestParam(required = false) String q) {
        List<UsuarioResponse> resultado = (q != null && !q.isBlank())
                ? usuarioService.buscarNoAdmins(q)
                : usuarioService.listarNoAdmins();
        return ResponseEntity.ok(resultado);
    }

    // PATCH /api/usuarios/{id}
// Edición parcial de atributos (sin contraseña) — solo sobre no-admins
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizarPorAdmin(
            @PathVariable Integer id,
            @Valid @RequestBody AdminEditUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPorAdmin(id, request));
    }

    // PATCH /api/usuarios/{id}/rol
// Cambio de rol dedicado: USUARIO ↔ OPERADOR
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> cambiarRol(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarRolRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request));
    }
}
