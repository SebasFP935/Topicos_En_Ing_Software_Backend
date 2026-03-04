package com.upb.TSIS.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles_permisos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RolPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 80)
    private String nombreRol;

    @Column(nullable = false)
    @Builder.Default
    private Integer prioridad = 0;

    /**
     * Permisos granulares del rol en formato JSON.
     * Ejemplo: {"puede_cancelar":true,"horas_max_cancelacion":24,"reservas_simultaneas":1}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reglas_acceso", columnDefinition = "jsonb")
    private Object reglasAcceso;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
