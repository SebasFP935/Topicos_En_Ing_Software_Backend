package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.dto.response.ScanResponse;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.service.IReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final IReservaService reservaService;

    // POST /api/reservas
    // El usuario autenticado crea su propia reserva
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(usuario.getId(), request));
    }

    // GET /api/reservas/{id}
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    // GET /api/reservas/qr/{codigoQr}
    // Usado por el operador al escanear el QR en la barrera
    @GetMapping("/qr/{codigoQr}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> obtenerPorQr(@PathVariable String codigoQr) {
        return ResponseEntity.ok(reservaService.obtenerPorQr(codigoQr));
    }

    // GET /api/reservas/mis-reservas
    // El usuario autenticado ve su historial completo
    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponse>> misReservas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.listarPorUsuario(usuario.getId()));
    }

    // GET /api/reservas/usuario/{usuarioId}
    // Admin puede ver reservas de cualquier usuario
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarPorUsuario(
            @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(reservaService.listarPorUsuario(usuarioId));
    }

    // GET /api/reservas/hoy
    // Panel del operador/admin: todas las reservas activas de hoy
    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<ReservaResponse>> reservasDeHoy() {
        return ResponseEntity.ok(reservaService.listarActualesDeHoy());
    }

    // PATCH /api/reservas/{id}/cancelar
    // El propio usuario cancela su reserva
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.cancelar(id, usuario.getId()));
    }

    // PATCH /api/reservas/checkin/{codigoQr}
    // Operador o admin registra entrada escaneando el QR
    @PatchMapping("/checkin/{codigoQr}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> checkIn(
            @PathVariable String codigoQr,
            @AuthenticationPrincipal Usuario operador) {
        return ResponseEntity.ok(reservaService.checkIn(codigoQr, operador.getId()));
    }

    // PATCH /api/reservas/checkout/{codigoQr}
    // Operador o admin registra salida
    @PatchMapping("/checkout/{codigoQr}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ReservaResponse> checkOut(
            @PathVariable String codigoQr,
            @AuthenticationPrincipal Usuario operador) {
        return ResponseEntity.ok(reservaService.checkOut(codigoQr, operador.getId()));
    }

    /**
     * GET /api/reservas/escanear/{token}
     *
     * Endpoint sin autenticación — el token firmado ES la autenticación.
     * Se activa cuando el usuario escanea el QR con su cámara.
     * Determina automáticamente si es check-in o check-out.
     */
    @GetMapping("/escanear/{token}")
    public ResponseEntity<ScanResponse> escanear(@PathVariable String token) {
        return ResponseEntity.ok(reservaService.escanear(token));
    }
}
