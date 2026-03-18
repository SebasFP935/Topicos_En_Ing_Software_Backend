package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.AdminEditUsuarioRequest;
import com.upb.TSIS.dto.request.CambiarRolRequest;
import com.upb.TSIS.dto.request.UsuarioRequest;
import com.upb.TSIS.dto.response.UsuarioResponse;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.service.IUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ReglaNegocioException("Ya existe un usuario con el email: " + request.getEmail());
        }
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .telefono(request.getTelefono())
                .rol(request.getRol() != null ? request.getRol() : RolUsuario.USUARIO)
                .vehiculoPlaca(request.getVehiculoPlaca())
                .vehiculoModelo(request.getVehiculoModelo())
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Integer id) {
        return toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorRol(RolUsuario rol) {
        return usuarioRepository.findByRol(rol).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UsuarioResponse actualizar(Integer id, UsuarioRequest request) {
        Usuario usuario = buscarOFallar(id);

        if (request.getNombre()        != null) usuario.setNombre(request.getNombre());
        if (request.getApellido()      != null) usuario.setApellido(request.getApellido());
        if (request.getTelefono()      != null) usuario.setTelefono(request.getTelefono());
        if (request.getVehiculoPlaca() != null) usuario.setVehiculoPlaca(request.getVehiculoPlaca());
        if (request.getVehiculoModelo()!= null) usuario.setVehiculoModelo(request.getVehiculoModelo());
        if (request.getRol()           != null) usuario.setRol(request.getRol());
        if (request.getPassword()      != null) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Usuario usuario = buscarOFallar(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    // ── Helpers ──────────────────────────────────────────────

    private Usuario buscarOFallar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id: " + id));
    }

    // ── Guard reutilizable ────────────────────────────────────────────
    private Usuario buscarNoAdminOFallar(Integer id) {
        Usuario usuario = buscarOFallar(id);
        if (usuario.getRol() == RolUsuario.ADMIN) {
            throw new ReglaNegocioException(
                    "No está permitido editar cuentas con rol ADMIN."
            );
        }
        return usuario;
    }

    // ── Listar todos los no-admins ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarNoAdmins() {
        return usuarioRepository.findByRolNot(RolUsuario.ADMIN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Búsqueda con filtro ───────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> buscarNoAdmins(String termino) {
        if (termino == null || termino.isBlank()) {
            return listarNoAdmins();
        }
        return usuarioRepository
                .buscarNoAdminsPorTermino(RolUsuario.ADMIN, termino.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Edición general sin contraseña ───────────────────────────────
    @Override
    @Transactional
    public UsuarioResponse actualizarPorAdmin(Integer id, AdminEditUsuarioRequest request) {
        Usuario usuario = buscarNoAdminOFallar(id);   // ← guard: no tocar admins

        if (request.getEmail() != null) {
            // Verificar que el nuevo email no esté en uso por otro usuario
            usuarioRepository.findByEmail(request.getEmail())
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> {
                        throw new ReglaNegocioException(
                                "El email " + request.getEmail() + " ya está registrado."
                        );
                    });
            usuario.setEmail(request.getEmail());
        }
        if (request.getNombre()         != null) usuario.setNombre(request.getNombre());
        if (request.getApellido()       != null) usuario.setApellido(request.getApellido());
        if (request.getTipoDocumento()  != null) usuario.setTipoDocumento(request.getTipoDocumento());
        if (request.getNumeroDocumento()!= null) usuario.setNumeroDocumento(request.getNumeroDocumento());
        if (request.getTelefono()       != null) usuario.setTelefono(request.getTelefono());
        if (request.getVehiculoPlaca()  != null) usuario.setVehiculoPlaca(request.getVehiculoPlaca());
        if (request.getVehiculoModelo() != null) usuario.setVehiculoModelo(request.getVehiculoModelo());
        if (request.getActivo()         != null) usuario.setActivo(request.getActivo());
        // rol también se puede cambiar desde aquí (validado por @AssertTrue en el DTO)
        if (request.getRol()            != null) usuario.setRol(request.getRol());

        return toResponse(usuarioRepository.save(usuario));
    }

    // ── Cambio de rol dedicado ────────────────────────────────────────
    @Override
    @Transactional
    public UsuarioResponse cambiarRol(Integer id, CambiarRolRequest request) {
        Usuario usuario = buscarNoAdminOFallar(id);   // ← guard: no tocar admins
        usuario.setRol(request.getRol());
        log.info("Rol del usuario {} cambiado a {}", usuario.getEmail(), request.getRol());
        return toResponse(usuarioRepository.save(usuario));
    }

    public UsuarioResponse toResponse(Usuario u) {
        return UsuarioResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .telefono(u.getTelefono())
                .rol(u.getRol())
                .vehiculoPlaca(u.getVehiculoPlaca())
                .vehiculoModelo(u.getVehiculoModelo())
                .activo(u.getActivo())
                .ultimoAcceso(u.getUltimoAcceso())
                .creadoEn(u.getCreadoEn())
                .build();
    }
}
