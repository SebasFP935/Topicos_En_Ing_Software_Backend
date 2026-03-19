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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.crear(usuario.getId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaResponse>> misReservas(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.listarPorUsuario(usuario.getId()));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservaResponse>> listarPorUsuario(
            @PathVariable Integer usuarioId) {
        return ResponseEntity.ok(reservaService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<ReservaResponse>> reservasDeHoy() {
        return ResponseEntity.ok(reservaService.listarActualesDeHoy());
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.cancelar(id, usuario.getId()));
    }

    /**
     * Flujo de QR fisico del espacio.
     * Determina automaticamente check-in, check-out u ocupacion espontanea.
     */
    @GetMapping("/escanear/{codigoQrFisico}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ScanResponse> escanear(
            @PathVariable String codigoQrFisico,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(reservaService.escanear(codigoQrFisico, usuario.getId()));
    }
}
