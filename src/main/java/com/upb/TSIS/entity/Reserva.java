package com.upb.TSIS.entity;

import com.upb.TSIS.entity.enums.EstadoReserva;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservas")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    /** Timestamp exacto de inicio, derivado de la franja horaria seleccionada */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(15) CHECK (estado IN ('ACTIVA','COMPLETADA','CANCELADA','NO_SHOW'))")
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    /** UUID único para validar acceso vía lector QR o barrera */
    @Column(name = "codigo_qr", nullable = false, unique = true, updatable = false, length = 36)
    @Builder.Default
    private String codigoQr = UUID.randomUUID().toString();

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vehiculo", nullable = false, columnDefinition = "VARCHAR(20) CHECK (tipo_vehiculo IN ('AUTO','MOTO','DISCAPACITADO','ELECTRICO'))")
    private TipoVehiculo tipoVehiculo;


    @PrePersist
    protected void onCreate() {
        this.creadoEn      = LocalDateTime.now();
        this.actualizadoEn = LocalDateTime.now();
        if (this.codigoQr == null) {
            this.codigoQr = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
