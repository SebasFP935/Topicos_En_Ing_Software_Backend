package com.upb.TSIS.repository;

import com.upb.TSIS.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByUsuario_IdOrderByFechaEnvioDesc(Integer usuarioId);

    List<Notificacion> findByUsuario_IdAndLeidaFalse(Integer usuarioId);

    long countByUsuario_IdAndLeidaFalse(Integer usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    void marcarTodasComoLeidas(Integer usuarioId);
}
