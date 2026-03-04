package com.upb.TSIS.entity;

import com.upb.TSIS.entity.enums.EstadoPenalizacion;
import com.upb.TSIS.entity.enums.TipoPenalizacion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "penalizaciones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Penalizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoPenalizacion tipo;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate fecha = LocalDate.now();

    @Column(name = "puntos_descontados", nullable = false)
    @Builder.Default
    private Integer puntosDescontados = 0;

    @Column(name = "fecha_expiracion")
    private LocalDate fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoPenalizacion estado = EstadoPenalizacion.ACTIVA;
}
