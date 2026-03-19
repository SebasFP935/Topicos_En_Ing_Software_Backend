package com.upb.TSIS;

import com.upb.TSIS.entity.ConfiguracionRegla;
import com.upb.TSIS.entity.Entidad;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import com.upb.TSIS.entity.enums.TipoRegla;
import com.upb.TSIS.repository.ConfiguracionReglaRepository;
import com.upb.TSIS.repository.EntidadRepository;
import com.upb.TSIS.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class Initializer implements CommandLineRunner {

    private final UsuarioRepository            usuarioRepository;
    private final EntidadRepository            entidadRepository;
    private final ConfiguracionReglaRepository reglaRepository;
    private final PasswordEncoder              passwordEncoder;

    // ── Admin configurable por variables de entorno / application.properties ──
    @Value("${app.init.admin.email:admin@tsis.com}")
    private String adminEmail;

    @Value("${app.init.admin.password:Admin123*}")
    private String adminPassword;

    @Value("${app.init.admin.nombre:Admin}")
    private String adminNombre;

    @Value("${app.init.admin.apellido:Principal}")
    private String adminApellido;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {

        // ── 1. Usuario administrador ────────────────────────────────────────
        if (usuarioRepository.count() == 0) {
            Usuario admin = Usuario.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .nombre(adminNombre)
                    .apellido(adminApellido)
                    .rol(RolUsuario.ADMIN)
                    .activo(true)
                    .build();
            usuarioRepository.save(admin);
            log.warn("▶ Admin creado: {}", adminEmail);
        }

        // ── 2. Entidad UPB ──────────────────────────────────────────────────
        // Solo hay un registro de entidad en todo el sistema.
        // configHorarios define las franjas que usan reservas y el frontend.
        if (entidadRepository.count() == 0) {

            List<Map<String, String>> horarios = List.of(
                    Map.of("codigo", "A", "inicio", "07:45", "fin", "09:45", "etiqueta", "Mañana 1"),
                    Map.of("codigo", "B", "inicio", "10:00", "fin", "12:00", "etiqueta", "Mañana 2"),
                    Map.of("codigo", "C", "inicio", "12:15", "fin", "14:15", "etiqueta", "Tarde 1"),
                    Map.of("codigo", "D", "inicio", "14:30", "fin", "16:30", "etiqueta", "Tarde 2"),
                    Map.of("codigo", "E", "inicio", "16:45", "fin", "18:45", "etiqueta", "Tarde 3"),
                    Map.of("codigo", "F", "inicio", "19:00", "fin", "21:00", "etiqueta", "Noche")
            );

            Entidad upb = Entidad.builder()
                    .nombre("Universidad Privada Boliviana (UPB)")
                    .direccion("Av. Villazón 1995, Cochabamba, Bolivia")
                    .telefono("+591 4 4293200")
                    .email("info@upb.edu")
                    .configHorarios(horarios)
                    .build();

            entidadRepository.save(upb);
            log.warn("▶ Entidad UPB creada con {} franjas horarias", horarios.size());
        }

        // ── 3. Reglas de negocio ────────────────────────────────────────────
        // El sistema usa findByTipoReglaAndActivaTrue(tipo) para leer cada regla,
        // por eso cada tipo debe tener exactamente un registro activo.
        if (reglaRepository.count() == 0) {

            // ── ANTICIPACION: cuántos días antes se puede reservar ──────────
            reglaRepository.save(ConfiguracionRegla.builder()
                    .tipoRegla(TipoRegla.ANTICIPACION)
                    .nombreRegla("Anticipación máxima de reserva")
                    .valorRegla(Map.of("max_dias", 2))
                    .activa(true)
                    .build());

            // ── HORARIO: franjas máximas por reserva + minutos de gracia ───
            // franjas_max: cuántas franjas contiguas puede tomar el usuario (ej: A+B = 2)
            // minutos_gracia: tiempo extra permitido antes de marcar NO_SHOW
            reglaRepository.save(ConfiguracionRegla.builder()
                    .tipoRegla(TipoRegla.HORARIO)
                    .nombreRegla("Reglas de horario de reserva")
                    .valorRegla(Map.of(
                            "franjas_max",     2,
                            "minutos_gracia",  15
                    ))
                    .activa(true)
                    .build());

            // ── PENALIZACION: puntos por incumplimiento ──────────────────
            // puntos_no_show:        no presentarse sin cancelar
            // puntos_cancel_tardia:  cancelar con menos de 30 min de anticipación
            // dias_expiracion:       cuántos días hasta que los puntos expiren
            reglaRepository.save(ConfiguracionRegla.builder()
                    .tipoRegla(TipoRegla.PENALIZACION)
                    .nombreRegla("Reglas de penalización por incumplimiento")
                    .valorRegla(Map.of(
                            "puntos_no_show",        10,
                            "puntos_cancel_tardia",   5,
                            "dias_expiracion",        30
                    ))
                    .activa(true)
                    .build());

            // ── PRIORIDAD: nivel de prioridad por rol ────────────────────
            // nivel 1 = mayor prioridad (reserva antes que otros roles)
            reglaRepository.save(ConfiguracionRegla.builder()
                    .tipoRegla(TipoRegla.PRIORIDAD)
                    .nombreRegla("Prioridad de reserva por rol de usuario")
                    .valorRegla(Map.of(
                            "rol",   "USUARIO",
                            "nivel", 1
                    ))
                    .activa(true)
                    .build());

            log.warn("▶ 4 reglas de negocio creadas (ANTICIPACION, HORARIO, PENALIZACION, PRIORIDAD)");
        }
    }
}