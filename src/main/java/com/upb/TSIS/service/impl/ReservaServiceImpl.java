package com.upb.TSIS.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upb.TSIS.dto.request.ReservaRequest;
import com.upb.TSIS.dto.response.ReservaResponse;
import com.upb.TSIS.dto.response.ScanResponse;
import com.upb.TSIS.entity.Entidad;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Penalizacion;
import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.EstadoPenalizacion;
import com.upb.TSIS.entity.enums.EstadoReserva;
import com.upb.TSIS.entity.enums.TipoPenalizacion;
import com.upb.TSIS.entity.enums.TipoRegla;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.ConfiguracionReglaRepository;
import com.upb.TSIS.repository.EntidadRepository;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.PenalizacionRepository;
import com.upb.TSIS.repository.ReservaRepository;
import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.service.INotificacionService;
import com.upb.TSIS.service.IReservaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.upb.TSIS.config.ParkingConstants.CHECKIN_VENTANA_ANTES_MIN;
import static com.upb.TSIS.config.ParkingConstants.CHECKIN_VENTANA_DESPUES_MIN;
import static com.upb.TSIS.config.ParkingConstants.CHECKOUT_VENTANA_EXTRA_MIN;
import static com.upb.TSIS.config.ParkingConstants.PENALIZACIONES_POR_INCREMENTO_PUNTO;
import static com.upb.TSIS.config.ParkingConstants.PUNTOS_BASE_PENALIZACION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements IReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final ConfiguracionReglaRepository reglaRepository;
    private final EntidadRepository entidadRepository;
    private final INotificacionService notificacionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PenalizacionRepository penalizacionRepository;

    @Override
    @Transactional
    public ReservaResponse crear(Integer usuarioId, ReservaRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);

        int maxDias = obtenerReglaNumerica(TipoRegla.ANTICIPACION, "max_dias", 2);
        if (request.getFechaReserva().isAfter(LocalDate.now().plusDays(maxDias))) {
            throw new ReglaNegocioException(
                    "No se puede reservar con mas de " + maxDias + " dia(s) de anticipacion.");
        }
        if (request.getFechaReserva().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se puede reservar para una fecha pasada.");
        }

        LocalDateTime inicio = resolverTimestamp(request.getFechaReserva(), request.getFranjaInicio(), true);
        LocalDateTime fin = resolverTimestamp(request.getFechaReserva(), request.getFranjaFin(), false);

        int maxFranjas = obtenerReglaNumerica(TipoRegla.HORARIO, "franjas_max", 2);
        long franjasSeleccionadas = contarFranjas(request.getFranjaInicio(), request.getFranjaFin());
        if (franjasSeleccionadas > maxFranjas) {
            throw new ReglaNegocioException("Maximo " + maxFranjas + " franja(s) por reserva.");
        }

        long reservasActivas = reservaRepository.contarReservasActivasTotales(usuarioId);
        if (reservasActivas >= 3) {
            throw new ReglaNegocioException(
                    "Has alcanzado el limite maximo de 3 reservas activas. Completa o cancela una antes de crear otra.");
        }

        if (reservaRepository.existeReservaActivaEnFecha(usuarioId, request.getFechaReserva())) {
            throw new ReglaNegocioException(
                    "Ya tienes una reserva activa para el " + request.getFechaReserva() + ". Solo se permite 1 reserva por dia.");
        }

        if (reservaRepository.existeSolapamientoUsuario(usuarioId, inicio, fin)) {
            throw new ReglaNegocioException(
                    "Ya tienes una reserva activa en ese horario. No puedes reservar dos espacios en la misma franja.");
        }

        Espacio espacio = seleccionarEspacio(request, inicio, fin);
        if (reservaRepository.existeSolapamiento(espacio.getId(), inicio, fin)) {
            throw new ReglaNegocioException("El espacio ya esta reservado en ese horario.");
        }

        TipoVehiculo tipoVehiculo = request.getTipoVehiculo() != null
                ? request.getTipoVehiculo()
                : espacio.getTipoVehiculo();

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .espacio(espacio)
                .fechaReserva(request.getFechaReserva())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .tipoVehiculo(tipoVehiculo)
                .estado(EstadoReserva.PENDIENTE_ACTIVACION)
                .build();

        Reserva guardada = reservaRepository.save(reserva);
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
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarActualesDeHoy() {
        return reservaRepository.findReservasActivasHoy()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReservaResponse cancelar(Integer id, Integer usuarioId) {
        Reserva reserva = buscarReserva(id);

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ReglaNegocioException("Solo el dueno puede cancelar su reserva.");
        }
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_ACTIVACION) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas pendientes de activacion.");
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
                .orElseThrow(() -> new RecursoNoEncontradoException("QR invalido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.PENDIENTE_ACTIVACION
                && !(reserva.getEstado() == EstadoReserva.ACTIVA && reserva.getCheckInTime() == null)) {
            throw new ReglaNegocioException("La reserva no esta disponible para hacer check-in.");
        }
        if (reserva.getCheckInTime() != null) {
            throw new ReglaNegocioException("La reserva ya registro check-in.");
        }

        int gracia = obtenerReglaNumerica(TipoRegla.HORARIO, "minutos_gracia", 15);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteEntrada = reserva.getFechaInicio().plusMinutes(gracia);
        if (ahora.isAfter(limiteEntrada)) {
            reserva.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            throw new ReglaNegocioException("Tiempo de gracia superado. La reserva fue marcada como NO_SHOW.");
        }

        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setCheckInTime(ahora);
        reserva.getEspacio().setEstado(EstadoEspacio.OCUPADO);
        espacioRepository.save(reserva.getEspacio());
        return toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Transactional
    public ReservaResponse checkOut(String codigoQr, Integer operadorId) {
        Reserva reserva = reservaRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("QR invalido: " + codigoQr));

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReglaNegocioException("La reserva no esta activa para hacer check-out.");
        }
        if (reserva.getCheckInTime() == null) {
            throw new ReglaNegocioException("La reserva todavia no registro check-in.");
        }

        reserva.setCheckOutTime(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.COMPLETADA);
        liberarEspacio(reserva.getEspacio());
        return toResponse(reservaRepository.save(reserva));
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void procesarReservasVencidas() {
        LocalDateTime ahora = LocalDateTime.now();

        LocalDateTime limiteEntrada = ahora.minusMinutes(CHECKIN_VENTANA_DESPUES_MIN);
        List<Reserva> noShows = reservaRepository.findActivasParaNoShow(limiteEntrada);
        for (Reserva reserva : noShows) {
            reserva.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            log.info("NO_SHOW automatico - reserva {} usuario {}", reserva.getId(), reserva.getUsuario().getId());
        }

        LocalDateTime limiteSalida = ahora.minusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);
        List<Reserva> tardias = reservaRepository.findActivasParaCheckoutTardio(limiteSalida);
        for (Reserva reserva : tardias) {
            aplicarPenalizacion(reserva, TipoPenalizacion.CANCELACION_TARDIA);
            reserva.setCheckOutTime(ahora);
            reserva.setEstado(EstadoReserva.COMPLETADA);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            log.warn("Checkout forzado con penalizacion - reserva {} usuario {}", reserva.getId(), reserva.getUsuario().getId());
        }
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void marcarEspaciosReservados() {
        List<Reserva> enCurso = reservaRepository.findReservasActivasEnCurso(LocalDateTime.now());
        for (Reserva reserva : enCurso) {
            Espacio espacio = reserva.getEspacio();
            if (espacio.getEstado() == EstadoEspacio.DISPONIBLE) {
                espacio.setEstado(EstadoEspacio.RESERVADO);
                espacioRepository.save(espacio);
            }
        }
    }

    @Override
    @Transactional
    public ScanResponse escanear(String codigoQrEspacio, Integer usuarioId) {
        Espacio espacio = espacioRepository.findByCodigoQr(codigoQrEspacio)
                .orElseThrow(() -> new ReglaNegocioException("El QR escaneado no corresponde a un espacio valido."));

        LocalDateTime ahora = LocalDateTime.now();
        Reserva reserva = seleccionarReservaParaEscaneo(usuarioId, espacio.getId(), ahora);

        if (reserva.getEstado() == EstadoReserva.ACTIVA && reserva.getCheckInTime() != null) {
            LocalDateTime limiteCheckout = reserva.getFechaFin().plusMinutes(CHECKOUT_VENTANA_EXTRA_MIN);
            if (ahora.isAfter(limiteCheckout)) {
                aplicarPenalizacion(reserva, TipoPenalizacion.CANCELACION_TARDIA);
            }

            reserva.setCheckOutTime(ahora);
            reserva.setEstado(EstadoReserva.COMPLETADA);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);

            return ScanResponse.builder()
                    .accion("CHECK_OUT")
                    .mensaje("Hasta pronto. Tu espacio quedo liberado.")
                    .estadoEspacio(EstadoEspacio.DISPONIBLE.name())
                    .codigoEspacio(reserva.getEspacio().getCodigo())
                    .zonaNombre(reserva.getEspacio().getZona().getNombre())
                    .timestamp(ahora)
                    .build();
        }

        LocalDateTime ventanaInicio = reserva.getFechaInicio().minusMinutes(CHECKIN_VENTANA_ANTES_MIN);
        LocalDateTime ventanaFin = reserva.getFechaInicio().plusMinutes(CHECKIN_VENTANA_DESPUES_MIN);

        if (ahora.isBefore(ventanaInicio)) {
            throw new ReglaNegocioException(
                    "Aun es demasiado temprano. Puedes activar tu reserva a partir de las "
                            + ventanaInicio.toLocalTime() + ".");
        }
        if (ahora.isAfter(ventanaFin)) {
            reserva.setEstado(EstadoReserva.NO_SHOW);
            liberarEspacio(reserva.getEspacio());
            reservaRepository.save(reserva);
            throw new ReglaNegocioException("La ventana de activacion expiro. La reserva fue marcada como NO_SHOW.");
        }

        reserva.setEstado(EstadoReserva.ACTIVA);
        reserva.setCheckInTime(ahora);
        reserva.getEspacio().setEstado(EstadoEspacio.OCUPADO);
        espacioRepository.save(reserva.getEspacio());
        reservaRepository.save(reserva);

        return ScanResponse.builder()
                .accion("CHECK_IN")
                .mensaje("Reserva activada. Ya puedes ocupar tu espacio.")
                .estadoEspacio(EstadoEspacio.OCUPADO.name())
                .codigoEspacio(reserva.getEspacio().getCodigo())
                .zonaNombre(reserva.getEspacio().getZona().getNombre())
                .timestamp(ahora)
                .build();
    }

    private Espacio seleccionarEspacio(ReservaRequest request, LocalDateTime inicio, LocalDateTime fin) {
        if (request.getEspacioId() != null) {
            Espacio espacio = espacioRepository.findById(request.getEspacioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + request.getEspacioId()));
            if (espacio.getEstado() == EstadoEspacio.BLOQUEADO
                    || espacio.getEstado() == EstadoEspacio.MANTENIMIENTO) {
                throw new ReglaNegocioException("El espacio solicitado no esta disponible.");
            }
            return espacio;
        }

        TipoVehiculo tipoVehiculo = request.getTipoVehiculo() != null ? request.getTipoVehiculo() : TipoVehiculo.AUTO;
        List<Espacio> disponibles = espacioRepository.findDisponibles(
                request.getZonaId(), tipoVehiculo, inicio, fin);
        if (disponibles.isEmpty()) {
            throw new ReglaNegocioException("No hay espacios disponibles para el horario solicitado.");
        }
        return disponibles.get(0);
    }

    private void liberarEspacio(Espacio espacio) {
        if (espacio.getEstado() == EstadoEspacio.RESERVADO
                || espacio.getEstado() == EstadoEspacio.OCUPADO) {
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
                    .filter(horario -> codigoFranja.equalsIgnoreCase(horario.get("codigo")))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Franja horaria invalida: " + codigoFranja));

            String hora = esInicio ? franja.get("inicio") : franja.get("fin");
            return LocalDateTime.of(fecha, java.time.LocalTime.parse(hora));
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
        return (fin - inicio) + 1L;
    }

    private int obtenerReglaNumerica(TipoRegla tipo, String campo, int defaultVal) {
        return reglaRepository.findByTipoReglaAndActivaTrue(tipo)
                .map(regla -> {
                    try {
                        Map<?, ?> valores = objectMapper.convertValue(regla.getValorRegla(), Map.class);
                        Object valor = valores.get(campo);
                        return valor != null ? Integer.parseInt(valor.toString()) : defaultVal;
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

    private Reserva seleccionarReservaParaEscaneo(Integer usuarioId, Integer espacioId, LocalDateTime ahora) {
        List<Reserva> reservas = reservaRepository.findByUsuario_IdAndEspacio_IdAndEstadoInOrderByFechaInicioAsc(
                usuarioId,
                espacioId,
                List.of(EstadoReserva.PENDIENTE_ACTIVACION, EstadoReserva.ACTIVA)
        );

        if (reservas.isEmpty()) {
            throw new ReglaNegocioException("El QR escaneado no corresponde a una reserva tuya pendiente o activa.");
        }

        return reservas.stream()
                .filter(reserva -> reserva.getEstado() == EstadoReserva.ACTIVA
                        && reserva.getCheckInTime() != null
                        && reserva.getCheckOutTime() == null)
                .findFirst()
                .orElseGet(() -> reservas.stream()
                        .filter(reserva -> reserva.getCheckInTime() == null)
                        .min(Comparator.comparingLong(reserva -> distanciaAlInicio(reserva, ahora)))
                        .orElseThrow(() -> new ReglaNegocioException(
                                "El QR escaneado no corresponde a una reserva tuya pendiente o activa.")));
    }

    private long distanciaAlInicio(Reserva reserva, LocalDateTime ahora) {
        return Math.abs(java.time.Duration.between(ahora, reserva.getFechaInicio()).toMinutes());
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

    public ReservaResponse toResponse(Reserva reserva) {
        Espacio espacio = reserva.getEspacio();
        Zona zona = espacio.getZona();

        return ReservaResponse.builder()
                .id(reserva.getId())
                .codigoQr(reserva.getCodigoQr())
                .fechaReserva(reserva.getFechaReserva())
                .fechaInicio(reserva.getFechaInicio())
                .fechaFin(reserva.getFechaFin())
                .estado(reserva.getEstado())
                .checkInTime(reserva.getCheckInTime())
                .checkOutTime(reserva.getCheckOutTime())
                .codigoEspacio(espacio.getCodigo())
                .zonaNombre(zona.getNombre())
                .sedeNombre(zona.getSede().getNombre())
                .usuarioNombre(reserva.getUsuario().getNombre() + " " + reserva.getUsuario().getApellido())
                .usuarioEmail(reserva.getUsuario().getEmail())
                .creadoEn(reserva.getCreadoEn())
                .build();
    }
}
