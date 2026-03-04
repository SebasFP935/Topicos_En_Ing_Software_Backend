package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    List<Reserva> findByUsuario_Id(Integer usuarioId);

    List<Reserva> findByUsuario_IdAndEstado(Integer usuarioId, EstadoReserva estado);

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
              AND r.estado = 'ACTIVA'
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
              AND r.estado NOT IN ('CANCELADA', 'NO_SHOW')
            ORDER BY r.fechaInicio
            """)
    List<Reserva> findReservasActivasHoy();

    // Reservas que ya pasaron su fin pero siguen ACTIVAS (para el job de expiración)
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.estado = 'ACTIVA'
              AND r.fechaFin < :ahora
            """)
    List<Reserva> findReservasExpiradas(LocalDateTime ahora);
}
