package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Integer> {

    List<Espacio> findByZona_Id(Integer zonaId);

    List<Espacio> findByZona_IdAndEstado(Integer zonaId, EstadoEspacio estado);

    List<Espacio> findByEstado(EstadoEspacio estado);

    // Espacios que no tienen reservas activas en el rango de fechas pedido
    @Query("""
            SELECT e FROM Espacio e
            WHERE e.zona.id = :zonaId
              AND e.estado = 'DISPONIBLE'
              AND e.tipoVehiculo = :tipoVehiculo
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
