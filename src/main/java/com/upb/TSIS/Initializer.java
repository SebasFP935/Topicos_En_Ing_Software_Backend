package com.upb.TSIS;

import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Initializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Admin
    @Value("${app.init.admin.email:admin@tsis.com}")
    private String adminEmail;

    @Value("${app.init.admin.password:Admin123*}")
    private String adminPassword;

    @Value("${app.init.admin.nombre:Admin}")
    private String adminNombre;

    @Value("${app.init.admin.apellido:Principal}")
    private String adminApellido;

    // Operador
    @Value("${app.init.operador.email:operador@tsis.com}")
    private String operadorEmail;

    @Value("${app.init.operador.password:Operador123*}")
    private String operadorPassword;

    @Value("${app.init.operador.nombre:Operador}")
    private String operadorNombre;

    @Value("${app.init.operador.apellido:Parking}")
    private String operadorApellido;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = Usuario.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .nombre(adminNombre)
                .apellido(adminApellido)
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build();

        usuarioRepository.save(admin);
        log.warn("Usuario administrador por defecto creado: {}", adminEmail);

        Usuario operador = Usuario.builder()
                .email(operadorEmail)
                .passwordHash(passwordEncoder.encode(operadorPassword))
                .nombre(operadorNombre)
                .apellido(operadorApellido)
                .rol(RolUsuario.OPERADOR)
                .activo(true)
                .build();

        usuarioRepository.save(operador);
        log.warn("Usuario operador por defecto creado: {}", operadorEmail);
    }
}