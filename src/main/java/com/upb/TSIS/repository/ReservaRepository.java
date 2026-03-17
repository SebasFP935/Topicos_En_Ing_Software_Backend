package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByUsuario_Id(Integer usuarioId);

    List<Reserva> findByUsuario_IdAndEstado(Integer usuarioId, EstadoReserva estado);

    List<Reserva> findByUsuario_IdAndEspacio_IdAndEstadoInOrderByFechaInicioAsc(
            Integer usuarioId,
            Integer espacioId,
            Collection<EstadoReserva> estados
    );

    List<Reserva> findByFechaReservaAndEstadoNot(LocalDate fecha, EstadoReserva estado);

    Optional<Reserva> findByCodigoQr(String codigoQr);

    // ¿Existe solapamiento para ese espacio en ese rango de tiempo?
    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.espacio.id = :espacioId
              AND r.estado NOT IN ('CANCELADA', 'NO_SHOW')
              AND r.fechaInicio < :fin
              AND r.fechaFin    > :inicio
            """)
    boolean existeSolapamiento(Integer espacioId, LocalDateTime inicio, LocalDateTime fin);

    // Cuántas reservas activas tiene el usuario hoy (para validar límite simultáneo)
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.usuario.id = :usuarioId
              AND r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
              AND r.fechaReserva = :fecha
            """)
    long contarReservasActivasEnFecha(Integer usuarioId, LocalDate fecha);

    // Reservas activas para el panel del operador (hoy)
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.usuario u
            JOIN FETCH r.espacio e
            JOIN FETCH e.zona z
            JOIN FETCH z.sede s
            WHERE r.fechaReserva = CURRENT_DATE
              AND r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
            ORDER BY r.fechaInicio
            """)
    List<Reserva> findReservasActivasHoy();

    // Reservas expiradas (para marcar NO_SHOW)
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.espacio e
            WHERE r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
              AND r.fechaFin < :ahora
            """)
    List<Reserva> findReservasExpiradas(LocalDateTime ahora);

    /**
     * NUEVA QUERY — Reservas ACTIVAS cuya franja ya comenzó pero el espacio
     * todavía no fue marcado como RESERVADO (el scheduler aún no pasó).
     * Usado por marcarEspaciosReservados() para actualizar el estado visual
     * del espacio en tiempo real sin tocar la lógica de disponibilidad.
     */
    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.espacio e
            WHERE r.estado = 'PENDIENTE_ACTIVACION'
              AND r.checkInTime IS NULL
              AND r.fechaInicio <= :ahora
              AND r.fechaFin    >  :ahora
              AND e.estado = 'DISPONIBLE'
            """)
    List<Reserva> findReservasActivasEnCurso(LocalDateTime ahora);

    /**
     * Reservas ACTIVAS sin check-in cuyo inicio + ventana ya pasó → candidatas a NO_SHOW.
     * @param limiteCheckIn = now() - CHECKIN_VENTANA_DESPUES_MIN
     */
    @Query("""
    SELECT r FROM Reserva r
    WHERE r.estado = 'PENDIENTE_ACTIVACION'
      AND r.checkInTime IS NULL
      AND r.fechaInicio <= :limiteCheckIn
    """)
    List<Reserva> findActivasParaNoShow(@Param("limiteCheckIn") LocalDateTime limiteCheckIn);

    /**
     * Reservas ACTIVAS con check-in cuyo fin + ventana de gracia ya pasó → candidatas a penalización.
     * @param limiteCheckOut = now() - CHECKOUT_VENTANA_EXTRA_MIN
     */
    @Query("""
    SELECT r FROM Reserva r
    WHERE r.estado = 'ACTIVA'
      AND r.checkInTime IS NOT NULL
      AND r.fechaFin <= :limiteCheckOut
    """)
    List<Reserva> findActivasParaCheckoutTardio(@Param("limiteCheckOut") LocalDateTime limiteCheckOut);

    // Total de reservas ACTIVAS del usuario (en cualquier fecha futura)
    @Query("""
        SELECT COUNT(r) FROM Reserva r
        WHERE r.usuario.id = :usuarioId
          AND r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
        """)
    long contarReservasActivasTotales(Integer usuarioId);

    // ¿El usuario ya tiene una reserva ACTIVA en esa fecha?
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.usuario.id = :usuarioId
          AND r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
          AND r.fechaReserva = :fecha
        """)
    boolean existeReservaActivaEnFecha(Integer usuarioId, LocalDate fecha);

    // ¿El usuario ya tiene una reserva ACTIVA que se solapa con ese rango horario?
    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.usuario.id = :usuarioId
          AND r.estado IN ('PENDIENTE_ACTIVACION', 'ACTIVA')
          AND r.fechaInicio < :fin
          AND r.fechaFin    > :inicio
        """)
    boolean existeSolapamientoUsuario(Integer usuarioId, LocalDateTime inicio, LocalDateTime fin);
}
