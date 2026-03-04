package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Entidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntidadRepository extends JpaRepository<Entidad, Integer> {

    // Siempre habrá un solo registro; este método facilita obtenerlo
    Optional<Entidad> findFirstBy();
}
