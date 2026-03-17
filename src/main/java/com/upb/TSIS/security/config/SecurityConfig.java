package com.upb.TSIS.security.config;

import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // habilita @PreAuthorize en controllers y services
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioRepository       usuarioRepository;
    private final CustomAuthEntryPoint    authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    // ── Cadena de filtros principal ──────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                // Desactivar CSRF (usamos JWT, no cookies de sesión)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Sin estado (stateless): JWT reemplaza la sesión HTTP
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Manejo de errores de autenticación/autorización
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // Reglas de autorización por ruta
                .authorizeHttpRequests(auth -> auth

                        // ── Rutas públicas ──────────────────────────
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        // ── Escaneo del QR ──────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/reservas/escanear/**").authenticated()

                        // ── Rutas solo ADMIN ────────────────────────
                        .requestMatchers(HttpMethod.POST,   "/api/sedes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/sedes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/sedes/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST,   "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/zonas/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST,   "/api/espacios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/espacios/**").hasRole("ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // ── Rutas ADMIN y OPERADOR ──────────────────
                        .requestMatchers("/api/bloqueos/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/hoy").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/checkin/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/checkout/**").hasAnyRole("ADMIN", "OPERADOR")

                        // ── El resto requiere solo estar autenticado ─
                        .anyRequest().authenticated()
                )

                // Registrar el filtro JWT antes del filtro de usuario/contraseña
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Usar nuestro AuthenticationProvider
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ── CORS ─────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // En desarrollo permite el origen de React; en producción ajustar
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Beans de seguridad ───────────────────────────────────

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
