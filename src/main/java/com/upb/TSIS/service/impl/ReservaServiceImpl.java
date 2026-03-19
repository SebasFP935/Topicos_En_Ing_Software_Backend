package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.response.ScanResponse;
import com.upb.TSIS.entity.enums.*;
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
import java.time.LocalTime;
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
            throw new ReglaNegocioException("Solo el dueno puede cancelar su reserva.");
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
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void procesarReservasVencidas() {
        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime limiteEntrada = ahora.minusMinutes(CHECKIN_VENTANA_DESPUES_MIN);
        List<Reserva> noShows = reservaRepository.findActivasParaNoShow(limiteEntrada);
        for (Reserva r : noShows) {
            r.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(r.getEspacio());
            reservaRepository.save(r);
            log.info("NO_SHOW automatico - reserva {} usuario {}", r.getId(), r.getUsuario().getId());
        }

        LocalDateTime limiteSalida = ahora.minusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);
        List<Reserva> tardias = reservaRepository.findActivasParaCheckoutTardio(limiteSalida);
        for (Reserva r : tardias) {
            aplicarPenalizacion(r, TipoPenalizacion.CANCELACION_TARDIA);
            r.setCheckOutTime(ahora);
            r.setEstado(EstadoReserva.COMPLETADA);
            liberarEspacio(r.getEspacio());
            reservaRepository.save(r);
            log.warn("Checkout forzado con penalizacion - reserva {} usuario {}", r.getId(), r.getUsuario().getId());
        }
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void marcarEspaciosReservados() {
        List<Reserva> enCurso = reservaRepository.findReservasActivasEnCurso(LocalDateTime.now());
        for (Reserva r : enCurso) {
            Espacio e = r.getEspacio();
            if (e.getEstado() == EstadoEspacio.DISPONIBLE) {
                e.setEstado(EstadoEspacio.RESERVADO);
                espacioRepository.save(e);
            }
        }
    }

    @Override
    @Transactional
    public ScanResponse escanear(String codigoQrFisicoEspacio, Integer usuarioId) {
        if (codigoQrFisicoEspacio == null || codigoQrFisicoEspacio.isBlank()) {
            throw new ReglaNegocioException("Codigo QR invalido.");
        }

        Usuario usuario = buscarUsuario(usuarioId);
        Espacio espacio = espacioRepository.findByCodigoQrFisicoIgnoreCase(codigoQrFisicoEspacio.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("El QR del espacio no existe."));

        LocalDateTime ahora = LocalDateTime.now();
        List<Reserva> reservasActivas = reservaRepository.findReservasActivasDeUsuarioEnEspacio(usuarioId, espacio.getId());

        Reserva reservaParaCheckout = reservasActivas.stream()
                .filter(r -> r.getCheckInTime() != null && r.getCheckOutTime() == null)
                .reduce((first, second) -> second)
                .orElse(null);

        if (reservaParaCheckout != null) {
            LocalDateTime limiteCheckout = reservaParaCheckout.getFechaFin().plusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);
            if (ahora.isAfter(limiteCheckout)) {
                aplicarPenalizacion(reservaParaCheckout, TipoPenalizacion.CANCELACION_TARDIA);
            }

            reservaParaCheckout.setCheckOutTime(ahora);
            reservaParaCheckout.setEstado(EstadoReserva.COMPLETADA);
            liberarEspacio(reservaParaCheckout.getEspacio());
            reservaRepository.save(reservaParaCheckout);

            return ScanResponse.builder()
                    .accion("CHECK_OUT")
                    .mensaje("Hasta pronto! Tu salida fue registrada y el espacio quedo liberado.")
                    .estadoEspacio("DISPONIBLE")
                    .codigoEspacio(reservaParaCheckout.getEspacio().getCodigo())
                    .zonaNombre(reservaParaCheckout.getEspacio().getZona().getNombre())
                    .timestamp(ahora)
                    .build();
        }

        Reserva reservaParaCheckin = reservasActivas.stream()
                .filter(r -> r.getCheckInTime() == null)
                .filter(r -> {
                    LocalDateTime inicioVentana = r.getFechaInicio().minusMinutes(CHECKIN_VENTANA_ANTES_MIN);
                    LocalDateTime finVentana = r.getFechaInicio().plusMinutes(CHECKIN_VENTANA_DESPUES_MIN);
                    return !ahora.isBefore(inicioVentana) && !ahora.isAfter(finVentana);
                })
                .findFirst()
                .orElse(null);

        if (reservaParaCheckin != null) {
            reservaParaCheckin.setCheckInTime(ahora);
            reservaParaCheckin.getEspacio().setEstado(EstadoEspacio.OCUPADO);
            espacioRepository.save(reservaParaCheckin.getEspacio());
            reservaRepository.save(reservaParaCheckin);

            return ScanResponse.builder()
                    .accion("CHECK_IN")
                    .mensaje("Bienvenido! Tu check-in fue registrado correctamente.")
                    .estadoEspacio("OCUPADO")
                    .codigoEspacio(reservaParaCheckin.getEspacio().getCodigo())
                    .zonaNombre(reservaParaCheckin.getEspacio().getZona().getNombre())
                    .timestamp(ahora)
                    .build();
        }

        Reserva reservaVencida = reservasActivas.stream()
                .filter(r -> r.getCheckInTime() == null)
                .filter(r -> ahora.isAfter(r.getFechaInicio().plusMinutes(CHECKIN_VENTANA_DESPUES_MIN)))
                .reduce((first, second) -> second)
                .orElse(null);

        if (reservaVencida != null) {
            reservaVencida.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reservaVencida.getEspacio());
            reservaRepository.save(reservaVencida);
            throw new ReglaNegocioException("La ventana de check-in expiro. La reserva fue marcada como NO_SHOW.");
        }

        Reserva reservaFutura = reservasActivas.stream()
                .filter(r -> r.getCheckInTime() == null)
                .filter(r -> ahora.isBefore(r.getFechaInicio().minusMinutes(CHECKIN_VENTANA_ANTES_MIN)))
                .findFirst()
                .orElse(null);

        if (reservaFutura != null) {
            LocalDateTime inicioVentana = reservaFutura.getFechaInicio().minusMinutes(CHECKIN_VENTANA_ANTES_MIN);
            throw new ReglaNegocioException(
                    "Aun es temprano para hacer check-in en este espacio. " +
                            "Podras escanear desde " + inicioVentana.toLocalTime() + ".");
        }

        return crearOcupacionEspontanea(usuario, espacio, ahora);
    }

    private Espacio seleccionarEspacio(ReservaRequest req, LocalDateTime inicio, LocalDateTime fin) {
        if (req.getEspacioId() != null) {
            Espacio e = espacioRepository.findById(req.getEspacioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + req.getEspacioId()));
            if (e.getEstado() == EstadoEspacio.BLOQUEADO || e.getEstado() == EstadoEspacio.MANTENIMIENTO) {
                throw new ReglaNegocioException("El espacio solicitado no esta disponible.");
            }
            return e;
        }

        List<Espacio> disponibles = espacioRepository.findDisponibles(
                req.getZonaId(), req.getTipoVehiculo(), inicio, fin);
        if (disponibles.isEmpty()) {
            throw new ReglaNegocioException("No hay espacios disponibles para el horario solicitado.");
        }
        return disponibles.get(0);
    }

    private void liberarEspacio(Espacio espacio) {
        if (espacio.getEstado() == EstadoEspacio.RESERVADO || espacio.getEstado() == EstadoEspacio.OCUPADO) {
            espacio.setEstado(EstadoEspacio.DISPONIBLE);
            espacioRepository.save(espacio);
        }
    }

    @SuppressWarnings("unchecked")
    private LocalDateTime resolverTimestamp(LocalDate fecha, String codigoFranja, boolean esInicio) {
        Entidad entidad = entidadRepository.findFirstBy()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuracion de entidad no encontrada."));

        try {
            List<Map<String, String>> horarios = objectMapper.convertValue(entidad.getConfigHorarios(), List.class);

            Map<String, String> franja = horarios.stream()
                    .filter(h -> codigoFranja.equalsIgnoreCase(h.get("codigo")))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Franja horaria invalida: " + codigoFranja));

            String horaStr = esInicio ? franja.get("inicio") : franja.get("fin");
            return LocalDateTime.of(fecha, LocalTime.parse(horaStr));
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("Error al procesar la franja horaria: " + codigoFranja);
        }
    }

    private long contarFranjas(String franjaInicio, String franjaFin) {
        char inicio = franjaInicio.toUpperCase().charAt(0);
        char fin = franjaFin.toUpperCase().charAt(0);
        if (fin < inicio) {
            throw new ReglaNegocioException("La franja fin no puede ser anterior a la franja inicio.");
        }
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

    private ScanResponse crearOcupacionEspontanea(Usuario usuario, Espacio espacio, LocalDateTime ahora) {
        if (espacio.getEstado() == EstadoEspacio.BLOQUEADO || espacio.getEstado() == EstadoEspacio.MANTENIMIENTO) {
            throw new ReglaNegocioException("Este espacio no puede ocuparse porque esta bloqueado o en mantenimiento.");
        }

        FranjaActual franjaActual = resolverFranjaActual(ahora);

        if (reservaRepository.existeSolapamientoActivoEspacio(espacio.getId(), franjaActual.inicio(), franjaActual.fin())) {
            throw new ReglaNegocioException(
                    "Este espacio ya se encuentra reservado u ocupado en el turno actual.");
        }

        if (reservaRepository.existeSolapamientoUsuario(usuario.getId(), franjaActual.inicio(), franjaActual.fin())) {
            throw new ReglaNegocioException(
                    "Ya tienes una reserva activa en este turno. Completa esa reserva antes de ocupar otro espacio.");
        }

        Reserva ocupacionEspontanea = Reserva.builder()
                .usuario(usuario)
                .espacio(espacio)
                .fechaReserva(franjaActual.inicio().toLocalDate())
                .fechaInicio(franjaActual.inicio())
                .fechaFin(franjaActual.fin())
                .tipoVehiculo(espacio.getTipoVehiculo() != null ? espacio.getTipoVehiculo() : TipoVehiculo.AUTO)
                .estado(EstadoReserva.ACTIVA)
                .checkInTime(ahora)
                .build();

        reservaRepository.save(ocupacionEspontanea);

        espacio.setEstado(EstadoEspacio.OCUPADO);
        espacioRepository.save(espacio);

        return ScanResponse.builder()
                .accion("CHECK_IN")
                .mensaje("Check-in registrado. Se creo una ocupacion espontanea para el turno actual.")
                .estadoEspacio("OCUPADO")
                .codigoEspacio(espacio.getCodigo())
                .zonaNombre(espacio.getZona().getNombre())
                .timestamp(ahora)
                .build();
    }

    @SuppressWarnings("unchecked")
    private FranjaActual resolverFranjaActual(LocalDateTime ahora) {
        Entidad entidad = entidadRepository.findFirstBy()
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuracion de entidad no encontrada."));

        try {
            List<Map<String, String>> horarios = objectMapper.convertValue(entidad.getConfigHorarios(), List.class);
            LocalTime horaActual = ahora.toLocalTime();

            Map<String, String> franja = horarios.stream()
                    .filter(h -> h.get("inicio") != null && h.get("fin") != null)
                    .filter(h -> {
                        LocalTime inicio = LocalTime.parse(h.get("inicio"));
                        LocalTime fin = LocalTime.parse(h.get("fin"));
                        return !horaActual.isBefore(inicio) && horaActual.isBefore(fin);
                    })
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException(
                            "No hay un turno horario activo en este momento para registrar ocupacion."));

            LocalDateTime inicio = LocalDateTime.of(ahora.toLocalDate(), LocalTime.parse(franja.get("inicio")));
            LocalDateTime fin = LocalDateTime.of(ahora.toLocalDate(), LocalTime.parse(franja.get("fin")));

            return new FranjaActual(
                    franja.getOrDefault("codigo", "SIN_CODIGO"),
                    inicio,
                    fin
            );
        } catch (ReglaNegocioException e) {
            throw e;
        } catch (Exception e) {
            throw new ReglaNegocioException("No se pudo resolver el turno horario actual.");
        }
    }

    private record FranjaActual(String codigo, LocalDateTime inicio, LocalDateTime fin) {
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

        return ReservaResponse.builder()
                .id(r.getId())
                .fechaReserva(r.getFechaReserva())
                .fechaInicio(r.getFechaInicio())
                .fechaFin(r.getFechaFin())
                .estado(r.getEstado())
                .checkInTime(r.getCheckInTime())
                .checkOutTime(r.getCheckOutTime())
                .codigoEspacio(e.getCodigo())
                .codigoQrFisico(e.getCodigoQrFisico())
                .zonaNombre(z.getNombre())
                .sedeNombre(z.getSede().getNombre())
                .usuarioNombre(r.getUsuario().getNombre() + " " + r.getUsuario().getApellido())
                .usuarioEmail(r.getUsuario().getEmail())
                .creadoEn(r.getCreadoEn())
                .build();
    }
}



