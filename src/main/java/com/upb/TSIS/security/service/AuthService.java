package com.upb.TSIS.security.service;

import com.upb.TSIS.entity.TokenRefresco;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.TokenRefrescoRepository;
import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.security.dto.AuthResponse;
import com.upb.TSIS.security.dto.LoginRequest;
import com.upb.TSIS.security.dto.RefreshRequest;
import com.upb.TSIS.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRefrescoRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // -- Registro -------------------------------------------------

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ReglaNegocioException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .tipoDocumento(request.getTipoDocumento())
                .numeroDocumento(request.getNumeroDocumento())
                .telefono(request.getTelefono())
                .vehiculoPlaca(request.getVehiculoPlaca())
                .vehiculoModelo(request.getVehiculoModelo())
                .rol(request.getRol() != null ? request.getRol() : RolUsuario.USUARIO)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Nuevo usuario registrado: {}", guardado.getEmail());

        return generarRespuestaCompleta(guardado);
    }

    // -- Login ----------------------------------------------------

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        tokenRepository.revocarTodosDeUsuario(usuario.getId());
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);

        log.info("Login exitoso: {}", usuario.getEmail());
        return generarRespuestaCompleta(usuario);
    }

    // -- Refresh token -------------------------------------------

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        TokenRefresco tokenEntity = tokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ReglaNegocioException("Refresh token no encontrado."));

        if (tokenEntity.getRevocado()) {
            throw new ReglaNegocioException("Refresh token revocado.");
        }
        if (tokenEntity.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("Refresh token expirado. Por favor inicia sesion nuevamente.");
        }

        tokenEntity.setRevocado(true);
        tokenRepository.save(tokenEntity);

        Usuario usuario = usuarioRepository.findById(tokenEntity.getUsuario().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        return generarRespuestaCompleta(usuario);
    }

    // -- Logout ---------------------------------------------------

    @Transactional
    public void logout(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));

        tokenRepository.revocarTodosDeUsuario(usuarioId);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);

        log.info("Logout: tokens revocados para usuarioId={}", usuarioId);
    }

    // -- Helpers --------------------------------------------------

    private AuthResponse generarRespuestaCompleta(Usuario usuario) {
        String accessToken = jwtService.generarAccessToken(usuario);
        String refreshToken = generarYGuardarRefreshToken(usuario);

        return new AuthResponse(
                accessToken,
                refreshToken,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol()
        );
    }

    private String generarYGuardarRefreshToken(Usuario usuario) {
        String tokenValue = UUID.randomUUID().toString();

        TokenRefresco token = TokenRefresco.builder()
                .usuario(usuario)
                .token(tokenValue)
                .expiraEn(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revocado(false)
                .build();

        tokenRepository.save(token);
        return tokenValue;
    }
}
