package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.QrPayload;
import com.upb.TSIS.dto.response.ScanResponse;
import com.upb.TSIS.entity.enums.*;
import com.upb.TSIS.exception.TokenQrInvalidoException;
import com.upb.TSIS.service.IQrTokenService;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.entity.*;
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
import java.util.List;
import java.util.Map;

import static com.upb.TSIS.config.ParkingConstants.*;
@Slf4j
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
    private final ObjectMapper             objectMapper = new ObjectMapper();
    private final IQrTokenService qrTokenService;
    private final PenalizacionRepository penalizacionRepository;

    @Override
    @Transactional
    public ReservaResponse crear(Integer usuarioId, ReservaRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);

        // 1. Validar anticipaciÃ³n mÃ¡xima
        int maxDias = obtenerReglaNumerica(TipoRegla.ANTICIPACION, "max_dias", 2);
        if (request.getFechaReserva().isAfter(LocalDate.now().plusDays(maxDias))) {
            throw new ReglaNegocioException(
                    "No se puede reservar con mÃ¡s de " + maxDias + " dÃ­a(s) de anticipaciÃ³n.");
        }
        if (request.getFechaReserva().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede reservar para una fecha pasada.");
        }

        // 2. Resolver franjas horarias desde config_horarios de la entidad
        LocalDateTime inicio = resolverTimestamp(request.getFechaReserva(), request.getFranjaInicio(), true);
        LocalDateTime fin    = resolverTimestamp(request.getFechaReserva(), request.getFranjaFin(), false);

        // 3. Validar mÃ¡ximo de franjas (duraciÃ³n)
        int maxFranjas = obtenerReglaNumerica(TipoRegla.HORARIO, "franjas_max", 2);
        long franjasSeleccionadas = contarFranjas(request.getFranjaInicio(), request.getFranjaFin());
        if (franjasSeleccionadas > maxFranjas) {
            throw new ReglaNegocioException("MÃ¡ximo " + maxFranjas + " franja(s) por reserva.");
        }

        // 3b. Validar lÃ­mite total de reservas activas (mÃ¡ximo 3)
        long reservasActivas = reservaRepository.contarReservasActivasTotales(usuarioId);
        if (reservasActivas >= 3) {
            throw new ReglaNegocioException(
                    "Has alcanzado el lÃ­mite mÃ¡ximo de 3 reservas activas. " +
                            "Completa o cancela una reserva existente antes de crear una nueva.");
        }

        // 3c. Validar lÃ­mite de 1 reserva por dÃ­a
        if (reservaRepository.existeReservaActivaEnFecha(usuarioId, request.getFechaReserva())) {
            throw new ReglaNegocioException(
                    "Ya tienes una reserva activa para el " + request.getFechaReserva() +
                            ". Solo se permite 1 reserva por dÃ­a.");
        }

        // 3d. Validar que el usuario no tenga otra reserva en el mismo horario (otro espacio, misma franja)
        if (reservaRepository.existeSolapamientoUsuario(usuarioId, inicio, fin)) {
            throw new ReglaNegocioException(
                    "Ya tienes una reserva activa en ese horario (" +
                            request.getFranjaInicio() + " - " + (request.getFranjaFin() != null ? request.getFranjaFin() : request.getFranjaInicio()) +
                            "). No puedes reservar dos espacios en el mismo horario.");
        }


        // 4. Seleccionar espacio (especÃ­fico o automÃ¡tico)
        Espacio espacio = seleccionarEspacio(request, inicio, fin);

        // 5. Verificar solapamiento final (doble chequeo de seguridad)
        if (reservaRepository.existeSolapamiento(espacio.getId(), inicio, fin)) {
            throw new ReglaNegocioException("El espacio ya estÃ¡ reservado en ese horario.");
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

        // 7. CORRECCIÃ“N: NO se cambia el estado fÃ­sico del espacio aquÃ­.
        //
        //    Antes: se marcaba RESERVADO globalmente al crear la reserva, lo que
        //    impedÃ­a reservar el mismo espacio en cualquier otro horario porque
        //    findDisponibles() filtraba por estado = DISPONIBLE.
        //
        //    Ahora: el estado del espacio solo cambia cuando la franja realmente
        //    comienza (scheduler marcarEspaciosReservados) o cuando el operador
        //    hace check-in (â†’ OCUPADO). La disponibilidad para otros horarios
        //    se controla exclusivamente mediante la consulta de solapamiento
        //    de reservas (existeSolapamiento / findDisponibles).

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
            throw new ReglaNegocioException("Solo el dueÃ±o puede cancelar su reserva.");
        }
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas activas.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        // Liberar solo si el espacio estaba marcado por esta reserva (puede que aÃºn no haya llegado la hora)
        liberarEspacio(reserva.getEspacio());
        Reserva guardada = reservaRepository.save(reserva);

        notificacionService.enviarCancelacionReserva(reserva.getUsuario(), guardada);
        return toResponse(guardada);
    }

    @Override
    @Transactional
    public ReservaResponse checkIn(String codigoQr, Integer operadorId) {
        Reserva reserva = reservaRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("QR invÃ¡lido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no estÃ¡ activa para hacer check-in.");
        }

        // Validar gracia: el check-in no puede ser despuÃ©s de inicio + minutos de gracia
        int gracia = obtenerReglaNumerica(TipoRegla.HORARIO, "minutos_gracia", 15);
        LocalDateTime limiteEntrada = reserva.getFechaInicio().plusMinutes(gracia);
        if (LocalDateTime.now().isAfter(limiteEntrada)) {
            reserva.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            throw new ReglaNegocioException("Tiempo de gracia superado. La reserva fue marcada como NO_SHOW.");
        }

        reserva.setCheckInTime(LocalDateTime.now());
        // Check-in fÃ­sico: ahora sÃ­ marcamos OCUPADO
        reserva.getEspacio().setEstado(EstadoEspacio.OCUPADO);
        espacioRepository.save(reserva.getEspacio());

        return toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public ReservaResponse checkOut(String codigoQr, Integer operadorId) {
        Reserva reserva = reservaRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("QR invÃ¡lido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no estÃ¡ activa para hacer check-out.");
        }

        reserva.setCheckOutTime(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.COMPLETADA);
        liberarEspacio(reserva.getEspacio());

        return toResponse(reservaRepository.save(reserva));
    }

    // â”€â”€ Schedulers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Reemplaza expirarReservasPasadas().
     * Separa los dos casos: no-show (sin penalizaciÃ³n) y checkout tardÃ­o (con penalizaciÃ³n).
     */
    @Override
    @Scheduled(fixedDelay = 60_000) // cada minuto
    @Transactional
    public void procesarReservasVencidas() {
        LocalDateTime ahora = LocalDateTime.now();

        // â”€â”€ Caso 1: NO_SHOW â€” sin check-in, ventana de entrada expirÃ³ â”€
        // Sin penalizaciÃ³n para el usuario.
        LocalDateTime limiteEntrada = ahora.minusMinutes(CHECKIN_VENTANA_DESPUES_MIN);
        List<Reserva> noShows = reservaRepository.findActivasParaNoShow(limiteEntrada);
        for (Reserva r : noShows) {
            r.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(r.getEspacio());
            reservaRepository.save(r);
            log.info("NO_SHOW automÃ¡tico â€” reserva {} usuario {}", r.getId(), r.getUsuario().getId());
        }

        // â”€â”€ Caso 2: Checkout tardÃ­o â€” con check-in, ventana de salida expirÃ³ â”€
        // Con penalizaciÃ³n escalada.
        LocalDateTime limiteSalida = ahora.minusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);
        List<Reserva> tardias = reservaRepository.findActivasParaCheckoutTardio(limiteSalida);
        for (Reserva r : tardias) {
            aplicarPenalizacion(r, TipoPenalizacion.CANCELACION_TARDIA);
            r.setCheckOutTime(ahora);
            r.setEstado(EstadoReserva.COMPLETADA);
            liberarEspacio(r.getEspacio());
            reservaRepository.save(r);
            log.warn("Checkout forzado con penalizaciÃ³n â€” reserva {} usuario {}", r.getId(), r.getUsuario().getId());
        }
    }

    /**
     * NUEVO SCHEDULER â€” Marca visualmente el espacio como RESERVADO
     * cuando la franja de la reserva comienza.
     *
     * Este scheduler es SOLO para el estado visual del mapa en tiempo real.
     * NO afecta la lÃ³gica de disponibilidad, que se basa en la consulta de
     * solapamiento de reservas (ver findDisponibles en EspacioRepository).
     */
    @Scheduled(fixedDelay = 60_000) // cada minuto
    @Transactional
    public void marcarEspaciosReservados() {
        List<Reserva> enCurso = reservaRepository.findReservasActivasEnCurso(LocalDateTime.now());
        for (Reserva r : enCurso) {
            Espacio e = r.getEspacio();
            // Solo marcar RESERVADO si el espacio estÃ¡ DISPONIBLE fÃ­sicamente
            // (no tocar OCUPADO, BLOQUEADO ni MANTENIMIENTO)
            if (e.getEstado() == EstadoEspacio.DISPONIBLE) {
                e.setEstado(EstadoEspacio.RESERVADO);
                espacioRepository.save(e);
            }
        }
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Espacio seleccionarEspacio(ReservaRequest req, LocalDateTime inicio, LocalDateTime fin) {
        if (req.getEspacioId() != null) {
            Espacio e = espacioRepository.findById(req.getEspacioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + req.getEspacioId()));
            // Solo rechazar estados administrativos permanentes
            if (e.getEstado() == EstadoEspacio.BLOQUEADO || e.getEstado() == EstadoEspacio.MANTENIMIENTO) {
                throw new ReglaNegocioException("El espacio solicitado no estÃ¡ disponible.");
            }
            // NOTA: No rechazamos por RESERVADO u OCUPADO aquÃ­.
            // El solapamiento temporal se verifica en el paso 5 (existeSolapamiento).
            return e;
        }
        // AsignaciÃ³n automÃ¡tica: primer disponible de la zona
        List<Espacio> disponibles = espacioRepository.findDisponibles(
                req.getZonaId(), req.getTipoVehiculo(), inicio, fin);
        if (disponibles.isEmpty()) {
            throw new ReglaNegocioException("No hay espacios disponibles para el horario solicitado.");
        }
        return disponibles.get(0);
    }

    /**
     * Libera el espacio fÃ­sicamente solo si fue marcado por una reserva
     * (RESERVADO u OCUPADO). No sobreescribe estados administrativos
     * como BLOQUEADO o MANTENIMIENTO.
     */
    private void liberarEspacio(Espacio espacio) {
        if (espacio.getEstado() == EstadoEspacio.RESERVADO
                || espacio.getEstado() == EstadoEspacio.OCUPADO) {
            espacio.setEstado(EstadoEspacio.DISPONIBLE);
            espacioRepository.save(espacio);
        }
        // BLOQUEADO y MANTENIMIENTO se ignoran intencionalmente:
        // esos estados los gestiona el admin, no el flujo de reservas.
    }

    /**
     * Resuelve el timestamp a partir del cÃ³digo de franja (A, B, Câ€¦)
     * leyendo config_horarios de la entidad.
     */
    @SuppressWarnings("unchecked")
    private LocalDateTime resolverTimestamp(LocalDate fecha, String codigoFranja, boolean esInicio) {
        Entidad entidad = entidadRepository.findFirstBy()
                .orElseThrow(() -> new RecursoNoEncontradoException("ConfiguraciÃ³n de entidad no encontrada."));

        try {
            List<Map<String, String>> horarios = objectMapper.convertValue(
                    entidad.getConfigHorarios(), List.class);

            Map<String, String> franja = horarios.stream()
                    .filter(h -> codigoFranja.equalsIgnoreCase(h.get("codigo")))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Franja horaria invÃ¡lida: " + codigoFranja));

            String horaStr = esInicio ? franja.get("inicio") : franja.get("fin");
            return LocalDateTime.of(fecha, java.time.LocalTime.parse(horaStr));
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("Error al procesar la franja horaria: " + codigoFranja);
        }
    }

    private long contarFranjas(String franjaInicio, String franjaFin) {
        char inicio = franjaInicio.toUpperCase().charAt(0);
        char fin    = franjaFin.toUpperCase().charAt(0);
        if (fin < inicio) throw new ReglaNegocioException("La franja fin no puede ser anterior a la franja inicio.");
        return (fin - inicio) + 1;
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

    @Override
    @Transactional
    public ScanResponse escanear(String token) {
        // 1. Validar firma â€” lanza TokenQrInvalidoException si es invÃ¡lido
        QrPayload payload = qrTokenService.validarToken(token);

        // 2. Cargar reserva y verificar consistencia bÃ¡sica
        Reserva reserva = reservaRepository.findById(payload.r())
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada."));

        if (!reserva.getUsuario().getId().equals(payload.u()) ||
                !reserva.getEspacio().getId().equals(payload.e())) {
            throw new TokenQrInvalidoException("El token no corresponde a esta reserva.");
        }

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no estÃ¡ activa (estado: " + reserva.getEstado() + ").");
        }

        LocalDateTime ahora = LocalDateTime.now();

        // â”€â”€ PRIMERA PASADA: Check-in â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (reserva.getCheckInTime() == null) {
            LocalDateTime ventanaInicio = reserva.getFechaInicio().minusMinutes(CHECKIN_VENTANA_ANTES_MIN);
            LocalDateTime ventanaFin    = reserva.getFechaInicio().plusMinutes(CHECKIN_VENTANA_DESPUES_MIN);

            if (ahora.isBefore(ventanaInicio)) {
                throw new ReglaNegocioException(
                        "AÃºn es demasiado temprano. Puedes hacer check-in a partir de las "
                                + ventanaInicio.toLocalTime() + ".");
            }
            if (ahora.isAfter(ventanaFin)) {
                // La ventana pasÃ³ â†’ NO_SHOW (el scheduler tambiÃ©n lo harÃ­a, pero respondemos claro)
                reserva.setEstado(EstadoReserva.NO_SHOW);
                liberarEspacio(reserva.getEspacio());
                reservaRepository.save(reserva);
                throw new ReglaNegocioException("La ventana de check-in expirÃ³. La reserva fue marcada como NO_SHOW.");
            }

            // Todo OK â†’ check-in
            reserva.setCheckInTime(ahora);
            reserva.getEspacio().setEstado(EstadoEspacio.OCUPADO);
            espacioRepository.save(reserva.getEspacio());
            reservaRepository.save(reserva);

            return ScanResponse.builder()
                    .accion("CHECK_IN")
                    .mensaje("Â¡Bienvenido! Tu espacio estÃ¡ activo.")
                    .estadoEspacio("OCUPADO")
                    .codigoEspacio(reserva.getEspacio().getCodigo())
                    .zonaNombre(reserva.getEspacio().getZona().getNombre())
                    .timestamp(ahora)
                    .build();
        }

        // â”€â”€ SEGUNDA PASADA: Check-out â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        LocalDateTime limiteCheckout = reserva.getFechaFin().plusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);

        if (ahora.isAfter(limiteCheckout)) {
            // Checkout tardÃ­o â€” aÃºn lo procesamos pero con penalizaciÃ³n
            aplicarPenalizacion(reserva, TipoPenalizacion.CANCELACION_TARDIA);
        }

        reserva.setCheckOutTime(ahora);
        reserva.setEstado(EstadoReserva.COMPLETADA);
        liberarEspacio(reserva.getEspacio());
        reservaRepository.save(reserva);

        return ScanResponse.builder()
                .accion("CHECK_OUT")
                .mensaje("Â¡Hasta pronto! Tu espacio quedÃ³ liberado.")
                .estadoEspacio("DISPONIBLE")
                .codigoEspacio(reserva.getEspacio().getCodigo())
                .zonaNombre(reserva.getEspacio().getZona().getNombre())
                .timestamp(ahora)
                .build();
    }

    private void aplicarPenalizacion(Reserva reserva, TipoPenalizacion tipo) {
        int penalizacionesActivas = penalizacionRepository
                .findByUsuario_IdAndEstado(reserva.getUsuario().getId(), EstadoPenalizacion.ACTIVA)
                .size();

        int puntosDescontados = PUNTOS_BASE_PENALIZACION
                + (penalizacionesActivas / PENALIZACIONES_POR_INCREMENTO_PUNTO);

        Penalizacion penalizacion = Penalizacion.builder()
                .usuario(reserva.getUsuario())
                .reserva(reserva)
                .tipo(tipo)
                .puntosDescontados(puntosDescontados)
                .estado(EstadoPenalizacion.ACTIVA)
                .build();

        penalizacionRepository.save(penalizacion);
    }

    public ReservaResponse toResponse(Reserva r) {
        Espacio e = r.getEspacio();
        Zona    z = e.getZona();

        String token = qrTokenService.generarToken(r);
        String url   = qrTokenService.generarUrlQr(r);

        return ReservaResponse.builder()
                .id(r.getId())
                .codigoQr(r.getCodigoQr())          // UUID interno (para operador manual)
                .qrToken(token)                      // token firmado para el QR
                .qrUrl(url)                          // URL a codificar como QR image
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
