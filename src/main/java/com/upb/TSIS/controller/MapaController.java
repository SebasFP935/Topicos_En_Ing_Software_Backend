package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.MapaRequest;
import com.upb.TSIS.dto.response.MapaResponse;
import com.upb.TSIS.service.impl.MapaServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zonas")
@RequiredArgsConstructor
public class MapaController {

    private final MapaServiceImpl mapaService;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /api/zonas/{id}/mapa
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Devuelve el plano completo: paredes + pasillos + espacios
    // con estado en tiempo real.
    // Acceso: cualquier usuario autenticado (admin y usuario final).
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/{id}/mapa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MapaResponse> obtenerMapa(@PathVariable Integer id) {
        return ResponseEntity.ok(mapaService.obtenerMapa(id));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /api/zonas/{id}/mapa
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // El admin guarda el estado completo del editor visual:
    // plano decorativo + todos los espacios con posiciÃ³n y tamaÃ±o.
    //
    // Body de ejemplo:
    // {
    //   "mapaAncho": 1200,
    //   "mapaAlto": 700,
    //   "plano": [
    //     { "type": "wall", "x": 50, "y": 40, "w": 900, "h": 10 },
    //     { "type": "road", "x": 50, "y": 200, "w": 900, "h": 60 }
    //   ],
    //   "espacios": [
    //     { "id": null, "codigo": "A-01", "tipoVehiculo": "AUTO",
    //       "x": 60, "y": 60, "w": 52, "h": 72 },
    //     { "id": 5, "x": 120, "y": 60, "w": 52, "h": 72 }
    //   ]
    // }
    //
    // Acceso: solo ADMIN.
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{id}/mapa")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<MapaResponse> guardarMapa(
            @PathVariable Integer id,
            @Valid @RequestBody MapaRequest request) {
        return ResponseEntity.ok(mapaService.guardarMapa(id, request));
    }
}
