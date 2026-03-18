package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Usuario;
import com.upb.TSIS.entity.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByRol(RolUsuario rol);

    List<Usuario> findByActivo(Boolean activo);

    // Todos los usuarios que NO sean ADMIN (para el dashboard de gestión)
    List<Usuario> findByRolNot(RolUsuario rol);

    // Búsqueda por nombre/apellido/email para el buscador del dashboard
    @Query("""
    SELECT u FROM Usuario u
    WHERE u.rol != :rolExcluido
      AND (
        LOWER(u.nombre)   LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(u.apellido) LIKE LOWER(CONCAT('%', :q, '%')) OR
        LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
      )
""")
    List<Usuario> buscarNoAdminsPorTermino(
            @Param("rolExcluido") RolUsuario rolExcluido,
            @Param("q") String q
    );
}
