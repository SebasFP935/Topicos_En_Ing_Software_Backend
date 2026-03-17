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

    @GetMapping
    public ResponseEntity<List<Sede>> listarActivas() {
        return ResponseEntity.ok(sedeService.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sede> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(sedeService.obtenerPorId(id));
    }

    // ADMIN o OPERADOR pueden crear sedes
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Sede> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(request));
    }

    // ADMIN o OPERADOR pueden editar sedes
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Sede> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody SedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizar(id, request));
    }

    // Solo ADMIN puede desactivar sedes
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        sedeService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}