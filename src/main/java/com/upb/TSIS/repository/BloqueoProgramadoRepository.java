package com.upb.TSIS.repository;

import com.upb.TSIS.entity.BloqueoProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoProgramadoRepository extends JpaRepository<BloqueoProgramado, Integer> {

    List<BloqueoProgramado> findByEspacio_Id(Integer espacioId);

    List<BloqueoProgramado> findByZona_Id(Integer zonaId);

    // Bloqueos vigentes en este momento para un espacio
    @Query("""
            SELECT b FROM BloqueoProgramado b
            WHERE b.espacio.id = :espacioId
              AND b.fechaInicioBloqueo <= :ahora
              AND (b.fechaFinBloqueo IS NULL OR b.fechaFinBloqueo >= :ahora)
            """)
    List<BloqueoProgramado> findBloqueoActivoPorEspacio(Integer espacioId, LocalDateTime ahora);

    // Bloqueos vigentes ahora mismo para una zona
    @Query("""
            SELECT b FROM BloqueoProgramado b
            WHERE b.zona.id = :zonaId
              AND b.fechaInicioBloqueo <= :ahora
              AND (b.fechaFinBloqueo IS NULL OR b.fechaFinBloqueo >= :ahora)
            """)
    List<BloqueoProgramado> findBloqueoActivoPorZona(Integer zonaId, LocalDateTime ahora);
}
