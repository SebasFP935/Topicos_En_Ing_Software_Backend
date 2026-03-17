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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UsuarioRepository         usuarioRepository;
    private final CustomAuthEntryPoint      authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth

                        // ── Públicas ──────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()

                        // ── Escaneo QR ────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/reservas/escanear/**").authenticated()

                        // ── Solo ADMIN: gestión de usuarios y dashboard
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // ── Solo ADMIN: borrar/desactivar sedes y zonas
                        .requestMatchers(HttpMethod.DELETE, "/api/sedes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/espacios/**").hasRole("ADMIN")

                        // ── ADMIN + OPERADOR: crear/editar sedes y zonas
                        .requestMatchers(HttpMethod.POST, "/api/sedes/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT,  "/api/sedes/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.POST, "/api/zonas/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers(HttpMethod.PUT,  "/api/zonas/**").hasAnyRole("ADMIN", "OPERADOR")

                        // ── Solo ADMIN: crear espacios via endpoint legacy
                        .requestMatchers(HttpMethod.POST, "/api/espacios/**").hasRole("ADMIN")

                        // ── ADMIN + OPERADOR: bloqueos, check-in/out, panel hoy
                        .requestMatchers("/api/bloqueos/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/hoy").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/checkin/**").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/reservas/checkout/**").hasAnyRole("ADMIN", "OPERADOR")

                        // ── El resto: solo autenticado ─────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}