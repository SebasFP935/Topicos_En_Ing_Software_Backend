package com.upb.TSIS.entity;

import com.upb.TSIS.entity.enums.EstadoEspera;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lista_espera")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_preferida_id")
    private Zona zonaPreferida;

    @Column(name = "fecha_solicitud", updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_deseada_inicio", nullable = false)
    private LocalDateTime fechaDeseadaInicio;

    @Column(name = "fecha_deseada_fin", nullable = false)
    private LocalDateTime fechaDeseadaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoEspera estado = EstadoEspera.ESPERANDO;

    @PrePersist
    protected void onCreate() {
        this.fechaSolicitud = LocalDateTime.now();
    }
}
