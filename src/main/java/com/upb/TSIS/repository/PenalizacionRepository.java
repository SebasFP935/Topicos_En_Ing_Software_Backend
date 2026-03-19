package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Penalizacion;
import com.upb.TSIS.entity.enums.EstadoPenalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenalizacionRepository extends JpaRepository<Penalizacion, Integer> {

    List<Penalizacion> findByUsuario_Id(Integer usuarioId);

    List<Penalizacion> findByUsuario_IdAndEstado(Integer usuarioId, EstadoPenalizacion estado);

    // Suma total de puntos de penalización activos de un usuario
    @Query(
            "SELECT COALESCE(SUM(p.puntosDescontados), 0) FROM Penalizacion p " +
            "WHERE p.usuario.id = :usuarioId " +
            "AND p.estado = 'ACTIVA'"
    )
    Integer sumPuntosActivosPorUsuario(Integer usuarioId);
}
