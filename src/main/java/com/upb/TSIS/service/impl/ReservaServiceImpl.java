package com.upb.TSIS.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.entity.*;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.EstadoReserva;
import com.upb.TSIS.entity.enums.TipoRegla;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.*;
import com.upb.TSIS.service.INotificacionService;
import com.upb.TSIS.service.IReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements IReservaService {

    private final ReservaRepository        reservaRepository;
    private final UsuarioRepository        usuarioRepository;
    private final EspacioRepository        espacioRepository;
    private final ZonaRepository           zonaRepository;
    private final ConfiguracionReglaRepository reglaRepository;
    private final EntidadRepository        entidadRepository;
    private final INotificacionService     notificacionService;
    private final ObjectMapper             objectMapper;

    @Override
    @Transactional
    public ReservaResponse crear(Integer usuarioId, ReservaRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);

        // 1. Validar anticipación máxima
        int maxDias = obtenerReglaNumerica(TipoRegla.ANTICIPACION, "max_dias", 2);
        if (request.getFechaReserva().isAfter(LocalDate.now().plusDays(maxDias))) {
            throw new ReglaNegocioException(
                    "No se puede reservar con más de " + maxDias + " día(s) de anticipación.");
        }
        if (request.getFechaReserva().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede reservar para una fecha pasada.");
        }

        // 2. Resolver franjas horarias desde config_horarios de la entidad
        LocalDateTime inicio = resolverTimestamp(request.getFechaReserva(), request.getFranjaInicio(), true);
        LocalDateTime fin    = resolverTimestamp(request.getFechaReserva(), request.getFranjaFin(), false);

        // 3. Validar máximo de franjas (duración)
        int maxFranjas = obtenerReglaNumerica(TipoRegla.HORARIO, "franjas_max", 2);
        long franjasSeleccionadas = contarFranjas(request.getFranjaInicio(), request.getFranjaFin());
        if (franjasSeleccionadas > maxFranjas) {
            throw new ReglaNegocioException("Máximo " + maxFranjas + " franja(s) por reserva.");
        }

        // 4. Seleccionar espacio (específico o automático)
        Espacio espacio = seleccionarEspacio(request, inicio, fin);

        // 5. Verificar solapamiento final
        if (reservaRepository.existeSolapamiento(espacio.getId(), inicio, fin)) {
            throw new ReglaNegocioException("El espacio ya está reservado en ese horario.");
        }

        // 6. Crear y persistir la reserva
        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .espacio(espacio)
                .fechaReserva(request.getFechaReserva())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .tipoVehiculo(request.getTipoVehiculo())
                .estado(EstadoReserva.ACTIVA)
                .build();

        Reserva guardada = reservaRepository.save(reserva);

        // 7. Marcar espacio como ocupado si la reserva es para ahora mismo
        espacio.setEstado(EstadoEspacio.RESERVADO);
        espacioRepository.save(espacio);

        // 8. Notificar al usuario
        notificacionService.enviarConfirmacionReserva(usuario, guardada);

        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorId(Integer id) {
        return toResponse(buscarReserva(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse obtenerPorQr(String codigoQr) {
        return reservaRepository.findByCodigoQr(codigoQr)
                .map(this::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada para QR: " + codigoQr));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorUsuario(Integer usuarioId) {
        return reservaRepository.findByUsuario_Id(usuarioId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarActualesDeHoy() {
        return reservaRepository.findReservasActivasHoy()
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ReservaResponse cancelar(Integer id, Integer usuarioId) {
        Reserva reserva = buscarReserva(id);

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ReglaNegocioException("Solo el dueño puede cancelar su reserva.");
        }
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas activas.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        liberarEspacio(reserva.getEspacio());
        Reserva guardada = reservaRepository.save(reserva);

        notificacionService.enviarCancelacionReserva(reserva.getUsuario(), guardada);
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public ReservaResponse checkIn(String codigoQr, Integer operadorId) {
        Reserva reserva = reservaRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("QR inválido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no está activa para hacer check-in.");
        }

        // Validar gracia: el check-in no puede ser después de inicio + minutos de gracia
        int gracia = obtenerReglaNumerica(TipoRegla.HORARIO, "minutos_gracia", 15);
        LocalDateTime limiteEntrada = reserva.getFechaInicio().plusMinutes(gracia);
        if (LocalDateTime.now().isAfter(limiteEntrada)) {
            reserva.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            throw new ReglaNegocioException("Tiempo de gracia superado. La reserva fue marcada como NO_SHOW.");
        }

        reserva.setCheckInTime(LocalDateTime.now());
        reserva.getEspacio().setEstado(EstadoEspacio.OCUPADO);
        espacioRepository.save(reserva.getEspacio());

        return toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public ReservaResponse checkOut(String codigoQr, Integer operadorId) {
        Reserva reserva = reservaRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("QR inválido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no está activa para hacer check-out.");
        }

        reserva.setCheckOutTime(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.COMPLETADA);
        liberarEspacio(reserva.getEspacio());

        return toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Scheduled(fixedDelay = 60_000) // cada minuto
    @Transactional
    public void expirarReservasPasadas() {
        List<Reserva> expiradas = reservaRepository.findReservasExpiradas(LocalDateTime.now());
        for (Reserva r : expiradas) {
            r.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(r.getEspacio());
            reservaRepository.save(r);
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private Espacio seleccionarEspacio(ReservaRequest req, LocalDateTime inicio, LocalDateTime fin) {
        if (req.getEspacioId() != null) {
            Espacio e = espacioRepository.findById(req.getEspacioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + req.getEspacioId()));
            if (e.getEstado() == EstadoEspacio.BLOQUEADO || e.getEstado() == EstadoEspacio.MANTENIMIENTO) {
                throw new ReglaNegocioException("El espacio solicitado no está disponible.");
            }
            return e;
        }
        // Asignación automática: primer disponible de la zona
        List<Espacio> disponibles = espacioRepository.findDisponibles(
                req.getZonaId(), req.getTipoVehiculo(), inicio, fin);
        if (disponibles.isEmpty()) {
            throw new ReglaNegocioException("No hay espacios disponibles para el horario solicitado.");
        }
        return disponibles.get(0);
    }

    private void liberarEspacio(Espacio espacio) {
        espacio.setEstado(EstadoEspacio.DISPONIBLE);
        espacioRepository.save(espacio);
    }

    /**
     * Resuelve el timestamp a partir del código de franja (A, B, C…)
     * leyendo config_horarios de la entidad.
     */
    @SuppressWarnings("unchecked")
    private LocalDateTime resolverTimestamp(LocalDate fecha, String codigoFranja, boolean esInicio) {
        Entidad entidad = entidadRepository.findFirstBy()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración de entidad no encontrada."));

        try {
            List<Map<String, String>> horarios = objectMapper.convertValue(
                    entidad.getConfigHorarios(), List.class);

            Map<String, String> franja = horarios.stream()
                    .filter(h -> codigoFranja.equalsIgnoreCase(h.get("codigo")))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Franja horaria inválida: " + codigoFranja));

            String horaStr = esInicio ? franja.get("inicio") : franja.get("fin");
            return LocalDateTime.of(fecha, LocalTime.parse(horaStr));
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("Error al procesar franjas horarias: " + e.getMessage());
        }
    }

    /** Cuenta cuántas franjas abarca la selección (A→B = 2 franjas). */
    private long contarFranjas(String inicio, String fin) {
        // Las franjas van de A a F (índice 0 a 5)
        int idxInicio = inicio.toUpperCase().charAt(0) - 'A';
        int idxFin    = fin.toUpperCase().charAt(0) - 'A';
        if (idxFin < idxInicio) throw new ReglaNegocioException("La franja de fin debe ser >= franja de inicio.");
        return (idxFin - idxInicio) + 1;
    }

    private int obtenerReglaNumerica(TipoRegla tipo, String campo, int defaultVal) {
        return reglaRepository.findByTipoReglaAndActivaTrue(tipo)
                .map(r -> {
                    try {
                        Map<?, ?> mapa = objectMapper.convertValue(r.getValorRegla(), Map.class);
                        Object val = mapa.get(campo);
                        return val != null ? Integer.parseInt(val.toString()) : defaultVal;
                    } catch (Exception e) {
                        return defaultVal;
                    }
                })
                .orElse(defaultVal);
    }

    private Usuario buscarUsuario(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    private Reserva buscarReserva(Integer id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + id));
    }

    public ReservaResponse toResponse(Reserva r) {
        Espacio e = r.getEspacio();
        Zona    z = e.getZona();
        return ReservaResponse.builder()
                .id(r.getId())
                .codigoQr(r.getCodigoQr())
                .fechaReserva(r.getFechaReserva())
                .fechaInicio(r.getFechaInicio())
                .fechaFin(r.getFechaFin())
                .estado(r.getEstado())
                .checkInTime(r.getCheckInTime())
                .checkOutTime(r.getCheckOutTime())
                .codigoEspacio(e.getCodigo())
                .zonaNombre(z.getNombre())
                .sedeNombre(z.getSede().getNombre())
                .usuarioNombre(r.getUsuario().getNombre() + " " + r.getUsuario().getApellido())
                .usuarioEmail(r.getUsuario().getEmail())
                .creadoEn(r.getCreadoEn())
                .build();
    }
}
