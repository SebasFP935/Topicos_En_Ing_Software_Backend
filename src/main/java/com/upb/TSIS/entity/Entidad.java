package com.upb.TSIS.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "entidad")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Entidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    /**
     * Franjas horarias operativas.
     * Formato: [{"codigo":"A","inicio":"07:45","fin":"09:45","etiqueta":"Mañana"}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_horarios", columnDefinition = "jsonb")
    private Object configHorarios;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
