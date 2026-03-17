package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.ZonaRequest;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.service.IZonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
public class ZonaController {

    private final IZonaService zonaService;

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<Zona>> listarPorSede(@PathVariable Integer sedeId) {
        return ResponseEntity.ok(zonaService.listarPorSede(sedeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Zona> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(zonaService.obtenerPorId(id));
    }

    // ADMIN o OPERADOR pueden crear zonas
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Zona> crear(@Valid @RequestBody ZonaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.crear(request));
    }

    // ADMIN o OPERADOR pueden editar zonas
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Zona> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ZonaRequest request) {
        return ResponseEntity.ok(zonaService.actualizar(id, request));
    }

    // Solo ADMIN puede desactivar zonas
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        zonaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}