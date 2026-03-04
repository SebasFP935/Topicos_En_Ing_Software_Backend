package com.upb.TSIS.entity;

import com.upb.TSIS.entity.enums.TipoRegla;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_reglas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConfiguracionRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regla", nullable = false, length = 20)
    private TipoRegla tipoRegla;

    @Column(name = "nombre_regla", nullable = false, length = 100)
    private String nombreRegla;

    /**
     * Valor configurable según tipo:
     * - ANTICIPACION : {"max_dias": 2}
     * - HORARIO      : {"franjas_max": 2, "minutos_gracia": 15}
     * - PENALIZACION : {"puntos_no_show": 10, "puntos_cancel_tardia": 5, "dias_expiracion": 30}
     * - PRIORIDAD    : {"rol": "ADMIN", "nivel": 1}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_regla", columnDefinition = "jsonb")
    private Object valorRegla;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
