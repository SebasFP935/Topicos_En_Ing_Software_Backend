package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.SedeRequest;
import com.upb.TSIS.entity.Sede;
import com.upb.TSIS.service.ISedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final ISedeService sedeService;

    // GET /api/sedes
    // Cualquier usuario autenticado puede ver las sedes activas
    @GetMapping
    public ResponseEntity<List<Sede>> listarActivas() {
        return ResponseEntity.ok(sedeService.listarActivas());
    }

    // GET /api/sedes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Sede> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(sedeService.obtenerPorId(id));
    }

    // POST /api/sedes  → solo ADMIN
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Sede> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(request));
    }

    // PUT /api/sedes/{id}  → solo ADMIN
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Sede> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody SedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizar(id, request));
    }

    // DELETE /api/sedes/{id}  → desactiva, solo ADMIN
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        sedeService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
