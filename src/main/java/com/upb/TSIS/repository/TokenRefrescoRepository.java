package com.upb.TSIS.repository;

import com.upb.TSIS.entity.TokenRefresco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Integer> {

    Optional<TokenRefresco> findByToken(String token);

    // Revoca todos los tokens activos de un usuario (útil al hacer logout)
    @Modifying
    @Query("UPDATE TokenRefresco t SET t.revocado = true WHERE t.usuario.id = :usuarioId AND t.revocado = false")
    void revocarTodosDeUsuario(Integer usuarioId);

    // Elimina tokens ya expirados o revocados para mantener la tabla limpia
    @Modifying
    @Query("DELETE FROM TokenRefresco t WHERE t.revocado = true OR t.expiraEn < CURRENT_TIMESTAMP")
    void limpiarTokensInvalidos();
}
