package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.BloqueoRequest;
import com.upb.TSIS.entity.BloqueoProgramado;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.service.IBloqueoProgramadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
public class BloqueoProgramadoController {

    private final IBloqueoProgramadoService bloqueoService;

    // POST /api/bloqueos  → ADMIN u OPERADOR
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<BloqueoProgramado> crear(
            @Valid @RequestBody BloqueoRequest request,
            @AuthenticationPrincipal Usuario admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bloqueoService.crear(admin.getId(), request));
    }

    // GET /api/bloqueos/espacio/{espacioId}
    @GetMapping("/espacio/{espacioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<BloqueoProgramado>> listarPorEspacio(
            @PathVariable Integer espacioId) {
        return ResponseEntity.ok(bloqueoService.listarPorEspacio(espacioId));
    }

    // GET /api/bloqueos/zona/{zonaId}
    @GetMapping("/zona/{zonaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<BloqueoProgramado>> listarPorZona(
            @PathVariable Integer zonaId) {
        return ResponseEntity.ok(bloqueoService.listarPorZona(zonaId));
    }

    // DELETE /api/bloqueos/{id}  → libera el bloqueo y restaura estado DISPONIBLE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        bloqueoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
