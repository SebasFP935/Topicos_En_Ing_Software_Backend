package com.upb.TSIS.repository;

import com.upb.TSIS.entity.ListaEspera;
import com.upb.TSIS.entity.enums.EstadoEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Integer> {

    List<ListaEspera> findByUsuario_Id(Integer usuarioId);

    // Primeros en espera para una zona dada, ordenados por fecha de solicitud (FIFO)
    List<ListaEspera> findByZonaPreferida_IdAndEstadoOrderByFechaSolicitudAsc(
            Integer zonaId, EstadoEspera estado);

    boolean existsByUsuario_IdAndEstado(Integer usuarioId, EstadoEspera estado);
}
