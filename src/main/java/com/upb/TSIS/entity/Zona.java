package com.upb.TSIS.entity;

import com.upb.TSIS.entity.enums.TipoZona;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "zonas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TipoZona tipo = TipoZona.DESCUBIERTO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Espacio> espacios;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
