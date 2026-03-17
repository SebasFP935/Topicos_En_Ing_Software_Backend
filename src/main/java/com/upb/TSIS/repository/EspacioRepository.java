package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Integer> {

    List<Espacio> findByZona_Id(Integer zonaId);

    Optional<Espacio> findByCodigoQr(String codigoQr);

    List<Espacio> findByZona_IdAndEstado(Integer zonaId, EstadoEspacio estado);

    List<Espacio> findByEstado(EstadoEspacio estado);

    /**
     * Espacios disponibles para reservar en el rango de tiempo dado.
     *
     * CORRECCIÓN: Ya NO filtramos por e.estado = 'DISPONIBLE' de forma global,
     * porque el estado físico del espacio puede ser RESERVADO u OCUPADO por una
     * franja diferente, y eso NO debe impedir reservar otra franja distinta.
     *
     * Solo excluimos BLOQUEADO y MANTENIMIENTO (decisión administrativa permanente).
     * El control de solapamiento temporal se delega completamente a la subconsulta
     * de reservas activas en ese rango.
     */
    @Query("""
            SELECT e FROM Espacio e
            WHERE e.zona.id = :zonaId
              AND e.tipoVehiculo = :tipoVehiculo
              AND e.estado NOT IN ('BLOQUEADO', 'MANTENIMIENTO')
              AND e.id NOT IN (
                  SELECT r.espacio.id FROM Reserva r
                  WHERE r.estado NOT IN ('CANCELADA', 'NO_SHOW')
                    AND r.fechaInicio < :fin
                    AND r.fechaFin    > :inicio
              )
            """)
    List<Espacio> findDisponibles(Integer zonaId, TipoVehiculo tipoVehiculo,
                                  LocalDateTime inicio, LocalDateTime fin);
}
