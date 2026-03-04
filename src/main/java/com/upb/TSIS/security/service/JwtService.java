package com.upb.TSIS.security.service;

import com.upb.TSIS.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ── Generación ───────────────────────────────────────────

    public String generarAccessToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol",      usuario.getRol().name());
        claims.put("nombre",   usuario.getNombre());
        claims.put("apellido", usuario.getApellido());
        claims.put("id",       usuario.getId());
        return buildToken(claims, usuario.getEmail(), expirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expMs) {
        Date ahora   = new Date();
        Date expira  = new Date(ahora.getTime() + expMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(getKey())
                .compact();
    }

    // ── Extracción de datos ──────────────────────────────────

    public String extraerEmail(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Integer extraerUsuarioId(String token) {
        return extraerClaim(token, claims -> claims.get("id", Integer.class));
    }

    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    public <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extraerTodosLosClaims(token));
    }

    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Validación ───────────────────────────────────────────

    public boolean esValido(String token, UserDetails userDetails) {
        try {
            String email = extraerEmail(token);
            return email.equals(userDetails.getUsername()) && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    public boolean estaExpirado(String token) {
        try {
            return extraerClaim(token, Claims::getExpiration).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    // ── Clave ────────────────────────────────────────────────

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}