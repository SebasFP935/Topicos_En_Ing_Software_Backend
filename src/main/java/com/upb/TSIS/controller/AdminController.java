package com.upb.TSIS.controller;

import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.dto.response.UsuarioResponse;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.service.IEspacioService;
import com.upb.TSIS.service.IReservaService;
import com.upb.TSIS.service.IUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IUsuarioService  usuarioService;
    private final IReservaService  reservaService;
    private final IEspacioService  espacioService;

    // GET /api/admin/dashboard
    // Métricas rápidas para el panel principal
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        long totalUsuarios   = usuarioService.listarTodos().size();
        long reservasHoy     = reservaService.listarActualesDeHoy().size();
        long totalOperadores = usuarioService.listarPorRol(RolUsuario.OPERADOR).size();

        return ResponseEntity.ok(Map.of(
                "totalUsuarios",   totalUsuarios,
                "reservasHoy",     reservasHoy,
                "totalOperadores", totalOperadores
        ));
    }

    // GET /api/admin/reservas/hoy
    // Vista completa de reservas del día para el panel
    @GetMapping("/reservas/hoy")
    public ResponseEntity<List<ReservaResponse>> reservasDeHoy() {
        return ResponseEntity.ok(reservaService.listarActualesDeHoy());
    }

    // GET /api/admin/usuarios/operadores
    // Lista de operadores para gestión
    @GetMapping("/usuarios/operadores")
    public ResponseEntity<List<UsuarioResponse>> listarOperadores() {
        return ResponseEntity.ok(usuarioService.listarPorRol(RolUsuario.OPERADOR));
    }

    // GET /api/admin/usuarios/estudiantes
    @GetMapping("/usuarios/estudiantes")
    public ResponseEntity<List<UsuarioResponse>> listarEstudiantes() {
        return ResponseEntity.ok(usuarioService.listarPorRol(RolUsuario.USUARIO));
    }
}
