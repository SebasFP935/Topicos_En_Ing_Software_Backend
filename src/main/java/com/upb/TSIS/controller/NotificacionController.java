package com.upb.TSIS.controller;

import com.upb.TSIS.entity.Notificacion;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.service.INotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final INotificacionService notificacionService;

    // GET /api/notificaciones
    // El usuario ve sus propias notificaciones
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notificacion>> misNotificaciones(
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(notificacionService.obtenerPorUsuario(usuario.getId()));
    }

    // GET /api/notificaciones/no-leidas/count
    // Badge del frontend con el conteo de no leídas
    @GetMapping("/no-leidas/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        long count = notificacionService.contarNoLeidas(usuario.getId());
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    // PATCH /api/notificaciones/marcar-leidas
    // Marca todas las notificaciones del usuario como leídas
    @PatchMapping("/marcar-leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> marcarTodasLeidas(
            @AuthenticationPrincipal Usuario usuario) {
        notificacionService.marcarTodasLeidas(usuario.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Notificaciones marcadas como leídas."));
    }
}
