package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.EspacioRequest;
import com.upb.TSIS.dto.response.EspacioQrResponse;
import com.upb.TSIS.dto.response.EspacioResponse;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.service.IEspacioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/espacios")
@RequiredArgsConstructor
public class EspacioController {

    private final IEspacioService espacioService;

    // GET /api/espacios/zona/{zonaId}
    // Todos los espacios de una zona (con su estado actual)
    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<EspacioResponse>> listarPorZona(@PathVariable Integer zonaId) {
        return ResponseEntity.ok(espacioService.listarPorZona(zonaId));
    }

    // GET /api/espacios/{id}
    @GetMapping("/{id}")
    public ResponseEntity<EspacioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(espacioService.obtenerPorId(id));
    }

    // GET /api/espacios/disponibles?zonaId=1&tipoVehiculo=AUTO&inicio=...&fin=...
    // Usado por el frontend para mostrar el mapa de disponibilidad en tiempo real
    @GetMapping("/disponibles")
    public ResponseEntity<List<EspacioResponse>> listarDisponibles(
            @RequestParam Integer zonaId,
            @RequestParam(defaultValue = "AUTO") TipoVehiculo tipoVehiculo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(espacioService.listarDisponibles(zonaId, tipoVehiculo, inicio, fin));
    }

    // POST /api/espacios  → solo ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(request));
    }

    // PATCH /api/espacios/{id}/estado  → ADMIN u OPERADOR
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<EspacioResponse> actualizarEstado(
            @PathVariable Integer id,
            @RequestParam EstadoEspacio estado) {
        return ResponseEntity.ok(espacioService.actualizarEstado(id, estado));
    }

    @PatchMapping("/{id}/codigo-qr/regenerar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EspacioQrResponse> regenerarCodigoQr(@PathVariable Integer id) {
        return ResponseEntity.ok(espacioService.regenerarCodigoQr(id));
    }

    // DELETE /api/espacios/{id}  → solo ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        espacioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
