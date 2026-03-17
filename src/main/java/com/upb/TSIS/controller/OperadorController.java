package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.AutoLayoutRequest;
import com.upb.TSIS.dto.request.EspacioOperadorRequest;
import com.upb.TSIS.dto.request.ImagenFondoRequest;
import com.upb.TSIS.dto.request.MapaRequest;
import com.upb.TSIS.dto.request.MoverEspacioRequest;
import com.upb.TSIS.dto.response.OperadorMapaResponse;
import com.upb.TSIS.service.IOperadorMapaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operador")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
public class OperadorController {

    private final IOperadorMapaService operadorMapaService;

    // ─────────────────────────────────────────────────────────────
    // IMAGEN DE FONDO
    // POST /api/operador/zonas/{id}/imagen-fondo
    // Body: { "imagenBase64": "data:image/png;base64,...", "imageWidth": 1200, "imageHeight": 700 }
    // Guarda la imagen en la zona y actualiza las dimensiones del canvas.
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/zonas/{id}/imagen-fondo")
    public ResponseEntity<Map<String, String>> subirImagenFondo(
            @PathVariable Integer id,
            @Valid @RequestBody ImagenFondoRequest request) {
        operadorMapaService.subirImagenFondo(id, request);
        return ResponseEntity.ok(Map.of("mensaje", "Imagen de fondo guardada correctamente."));
    }

    // ─────────────────────────────────────────────────────────────
    // AUTO-GENERAR ESPACIOS
    // POST /api/operador/zonas/{id}/generar-espacios
    // Body: { "columnas": 5, "filas": 3, "tipoVehiculo": "AUTO", ... }
    // Genera una grilla de espacios sobre la imagen de fondo.
    // Si reemplazar=true, elimina primero los espacios sin reservas activas.
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/zonas/{id}/generar-espacios")
    public ResponseEntity<OperadorMapaResponse> generarEspacios(
            @PathVariable Integer id,
            @RequestBody AutoLayoutRequest request) {
        return ResponseEntity.ok(operadorMapaService.generarEspaciosAutomatico(id, request));
    }

    // ─────────────────────────────────────────────────────────────
    // OBTENER MAPA
    // GET /api/operador/zonas/{id}/mapa
    // Devuelve: plano + espacios (con codigoQr) + imagenFondo
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/zonas/{id}/mapa")
    public ResponseEntity<OperadorMapaResponse> obtenerMapa(@PathVariable Integer id) {
        return ResponseEntity.ok(operadorMapaService.obtenerMapa(id));
    }

    // ─────────────────────────────────────────────────────────────
    // GUARDAR MAPA COMPLETO
    // PUT /api/operador/zonas/{id}/mapa
    // Misma semántica del editor admin: aplica diff de espacios y guarda plano.
    // ─────────────────────────────────────────────────────────────
    @PutMapping("/zonas/{id}/mapa")
    public ResponseEntity<OperadorMapaResponse> guardarMapa(
            @PathVariable Integer id,
            @Valid @RequestBody MapaRequest request) {
        return ResponseEntity.ok(operadorMapaService.guardarMapa(id, request));
    }

    // ─────────────────────────────────────────────────────────────
    // AÑADIR ESPACIO INDIVIDUAL
    // POST /api/operador/zonas/{id}/espacios
    // Body: { "codigo": "A-01", "tipoVehiculo": "AUTO", "x": 100, "y": 100, "w": 52, "h": 72 }
    // Crea el espacio y retorna su DTO con codigoQr incluido.
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/zonas/{id}/espacios")
    public ResponseEntity<OperadorMapaResponse.EspacioOperadorDto> agregarEspacio(
            @PathVariable Integer id,
            @Valid @RequestBody EspacioOperadorRequest request) {
        return ResponseEntity.ok(operadorMapaService.agregarEspacio(id, request));
    }

    // ─────────────────────────────────────────────────────────────
    // ELIMINAR ESPACIO
    // DELETE /api/operador/zonas/{id}/espacios/{eid}
    // Lanza 422 si el espacio tiene reservas activas.
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/zonas/{id}/espacios/{eid}")
    public ResponseEntity<Void> eliminarEspacio(
            @PathVariable Integer id,
            @PathVariable Integer eid) {
        operadorMapaService.eliminarEspacio(id, eid);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // MOVER / REDIMENSIONAR ESPACIO
    // PATCH /api/operador/zonas/{id}/espacios/{eid}/posicion
    // Body: { "x": 150.0, "y": 200.0, "w": 52.0, "h": 72.0 }
    // w y h son opcionales — si se omiten mantienen el valor actual.
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/zonas/{id}/espacios/{eid}/posicion")
    public ResponseEntity<OperadorMapaResponse.EspacioOperadorDto> moverEspacio(
            @PathVariable Integer id,
            @PathVariable Integer eid,
            @Valid @RequestBody MoverEspacioRequest request) {
        return ResponseEntity.ok(operadorMapaService.moverEspacio(id, eid, request));
    }

    // ─────────────────────────────────────────────────────────────
    // QR IMAGE DEL ESPACIO FÍSICO
    // GET /api/operador/espacios/{id}/qr-imagen
    // Retorna la imagen QR (data URL PNG base64) del UUID permanente del slot.
    // Usada para imprimir etiquetas físicas en los slots del parqueo.
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/espacios/{id}/qr-imagen")
    public ResponseEntity<Map<String, String>> obtenerQrImagen(@PathVariable Integer id) {
        String dataUrl = operadorMapaService.obtenerQrImagenEspacio(id);
        return ResponseEntity.ok(Map.of("qrDataUrl", dataUrl));
    }
}