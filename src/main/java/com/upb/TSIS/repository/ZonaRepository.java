package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Integer> {

    List<Zona> findBySede_Id(Integer sedeId);

    List<Zona> findBySede_IdAndActivoTrue(Integer sedeId);
}
