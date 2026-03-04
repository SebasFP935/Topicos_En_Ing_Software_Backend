package com.upb.TSIS.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bloqueos_programados")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BloqueoProgramado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * El bloqueo aplica a un espacio individual O a una zona completa, nunca a ambos.
     * Si espacio != null → bloqueo puntual.
     * Si zona    != null → bloquea todos los espacios de esa zona.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id")
    private Espacio espacio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id")
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Usuario admin;

    @Column(nullable = false, length = 255)
    private String motivo;

    @Column(name = "fecha_inicio_bloqueo", nullable = false)
    private LocalDateTime fechaInicioBloqueo;

    /** Null significa bloqueo indefinido */
    @Column(name = "fecha_fin_bloqueo")
    private LocalDateTime fechaFinBloqueo;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
