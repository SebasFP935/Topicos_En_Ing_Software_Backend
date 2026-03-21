package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.EspacioRequest;
import com.upb.TSIS.dto.response.EspacioResponse;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.service.IEspacioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping("/zona/{zonaId}")
    public ResponseEntity<List<EspacioResponse>> listarPorZona(@PathVariable Integer zonaId) {
        return ResponseEntity.ok(espacioService.listarPorZona(zonaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacioResponse> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(espacioService.obtenerPorId(id));
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<EspacioResponse>> listarDisponibles(
            @RequestParam Integer zonaId,
            @RequestParam(defaultValue = "AUTO") TipoVehiculo tipoVehiculo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(espacioService.listarDisponibles(zonaId, tipoVehiculo, inicio, fin));
    }

    /**
     * Descarga el QR físico del espacio como imagen PNG.
     * GET /api/espacios/{id}/qr
     */
    @GetMapping("/{id}/qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<byte[]> descargarQr(@PathVariable Integer id) {
        byte[] png = espacioService.obtenerQrPng(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"qr-espacio-" + id + ".png\"")
                .body(png);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<EspacioResponse> crear(@Valid @RequestBody EspacioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espacioService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<EspacioResponse> actualizarEstado(
            @PathVariable Integer id,
            @RequestParam EstadoEspacio estado) {
        return ResponseEntity.ok(espacioService.actualizarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        espacioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}