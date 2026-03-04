package com.upb.TSIS.repository;

import com.upb.TSIS.entity.RolPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, Integer> {

    Optional<RolPermiso> findByNombreRol(String nombreRol);
}
