package com.upb.TSIS.controller;

import com.upb.TSIS.dto.request.ListaEsperaRequest;
import com.upb.TSIS.entity.ListaEspera;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.service.IListaEsperaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lista-espera")
@RequiredArgsConstructor
public class ListaEsperaController {

    private final IListaEsperaService listaEsperaService;

    // POST /api/lista-espera
    // Usuario se agrega a la lista de espera de una zona
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ListaEspera> agregar(
            @Valid @RequestBody ListaEsperaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listaEsperaService.agregar(usuario.getId(), request));
    }

    // GET /api/lista-espera/mis-solicitudes
    // El usuario ve sus solicitudes en espera
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ListaEspera>> misSolicitudes(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(listaEsperaService.listarPorUsuario(usuario.getId()));
    }

    // DELETE /api/lista-espera/{id}
    // El usuario cancela su propia solicitud en espera
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cancelar(
            @PathVariable Integer id,
            @AuthenticationPrincipal Usuario usuario) {
        listaEsperaService.cancelar(id, usuario.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Solicitud cancelada correctamente."));
    }
}
