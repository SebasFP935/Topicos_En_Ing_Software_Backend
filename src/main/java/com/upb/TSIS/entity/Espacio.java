package com.upb.TSIS.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "espacios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @JsonIgnore
    @Column(name = "codigo_qr", nullable = false, unique = true, length = 36)
    @Builder.Default
    private String codigoQr = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) CHECK (estado IN ('DISPONIBLE','RESERVADO','OCUPADO','BLOQUEADO','MANTENIMIENTO'))")
    @Builder.Default
    private EstadoEspacio estado = EstadoEspacio.DISPONIBLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, columnDefinition = "VARCHAR(20) CHECK (tipo_vehiculo IN ('AUTO','MOTO','DISCAPACITADO','ELECTRICO'))")
    @Builder.Default
    private TipoVehiculo tipoVehiculo = TipoVehiculo.AUTO;

    /**
     * Posición del espacio en el mapa del parqueo.
     * Formato: {"x": 120.5, "y": 340.2}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object coordenadas;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @JsonIgnore
    @OneToMany(mappedBy = "espacio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reserva> reservas;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        if (this.codigoQr == null) {
            this.codigoQr = UUID.randomUUID().toString();
        }
    }

    public void regenerarCodigoQr() {
        this.codigoQr = UUID.randomUUID().toString();
    }
}
