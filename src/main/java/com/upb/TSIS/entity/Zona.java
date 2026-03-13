package com.upb.TSIS.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upb.TSIS.entity.enums.TipoZona;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) CHECK (tipo IN ('CUBIERTO','DESCUBIERTO','TECHADO'))")
    @Builder.Default
    private TipoZona tipo = TipoZona.DESCUBIERTO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Elementos decorativos del plano dibujados por el admin:
     * paredes y pasillos como rectángulos.
     *
     * Formato guardado en JSONB:
     * [
     *   { "type": "wall", "x": 50,  "y": 40,  "w": 900, "h": 10 },
     *   { "type": "road", "x": 50,  "y": 200, "w": 900, "h": 60 }
     * ]
     *
     * Null si la zona aún no tiene mapa configurado.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object plano;

    /**
     * Dimensiones del canvas donde se dibujó el plano.
     * El frontend usa estos valores para escalar correctamente
     * el mapa en pantallas de distinto tamaño.
     */
    @Column(name = "mapa_ancho")
    @Builder.Default
    private Integer mapaAncho = 1200;

    @Column(name = "mapa_alto")
    @Builder.Default
    private Integer mapaAlto = 700;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @JsonIgnore
    @OneToMany(mappedBy = "zona", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Espacio> espacios;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}