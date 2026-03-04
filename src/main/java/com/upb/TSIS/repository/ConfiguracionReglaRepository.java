package com.upb.TSIS.repository;

import com.upb.TSIS.entity.ConfiguracionRegla;
import com.upb.TSIS.entity.enums.TipoRegla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracionReglaRepository extends JpaRepository<ConfiguracionRegla, Integer> {

    Optional<ConfiguracionRegla> findByTipoReglaAndActivaTrue(TipoRegla tipoRegla);

    List<ConfiguracionRegla> findByActivaTrue();
}
