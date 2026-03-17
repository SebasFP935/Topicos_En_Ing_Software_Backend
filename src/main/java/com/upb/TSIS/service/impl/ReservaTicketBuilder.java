package com.upb.TSIS.service.impl;

import com.upb.TSIS.entity.Reserva;
import com.upb.TSIS.service.IQrImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Genera el HTML del ticket de reserva, listo para adjuntar como archivo
 * o embeber en el cuerpo del correo. El QR queda embebido como base64.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaTicketBuilder {

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FMT  = DateTimeFormatter.ofPattern("HH:mm");

    // Inyectado — ya no duplicamos la lógica de generación QR
    private final IQrImageService qrImageService;

    /**
     * Genera el HTML completo del ticket como String.
     * @param reserva entidad de la reserva (con espacio y zona cargados)
     * @param qrUrl   URL firmada que irá codificada en el QR
     */
    public String buildHtml(Reserva reserva, String qrUrl) {
        // Delegamos la generación QR al servicio compartido
        String qrBase64 = qrImageService.generarBase64(qrUrl);

        String nombre    = reserva.getUsuario().getNombre() + " " + reserva.getUsuario().getApellido();
        String espacio   = reserva.getEspacio().getCodigo();
        String zona      = reserva.getEspacio().getZona().getNombre();
        String sede      = reserva.getEspacio().getZona().getSede().getNombre();
        String fecha     = reserva.getFechaReserva().format(FECHA_FMT);
        String horaIni   = reserva.getFechaInicio().format(HORA_FMT);
        String horaFin   = reserva.getFechaFin().format(HORA_FMT);
        String reservaId = String.format("RES-%05d", reserva.getId());

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Tu Reserva de Parqueo</title>
              <link rel="preconnect" href="https://fonts.googleapis.com"/>
              <link href="https://fonts.googleapis.com/css2?family=DM+Mono:wght@400;500&family=Sora:wght@300;400;600;700&display=swap" rel="stylesheet"/>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

                body {
                  background: #0b0d14;
                  font-family: 'Sora', sans-serif;
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  min-height: 100vh;
                  padding: 40px 20px;
                }

                .wrapper {
                  width: 100%%;
                  max-width: 520px;
                }

                .header {
                  text-align: center;
                  margin-bottom: 24px;
                }
                .header-logo {
                  font-family: 'DM Mono', monospace;
                  font-size: 11px;
                  letter-spacing: 4px;
                  text-transform: uppercase;
                  color: #4a5068;
                  margin-bottom: 8px;
                }
                .header-title {
                  font-size: 22px;
                  font-weight: 700;
                  color: #e8ebf5;
                }

                .ticket {
                  background: #12141f;
                  border-radius: 20px;
                  overflow: hidden;
                  border: 1px solid #1e2130;
                  box-shadow: 0 24px 80px rgba(0,0,0,.5);
                }

                .ticket-band {
                  background: linear-gradient(135deg, #00C9A7, #0094ff);
                  padding: 20px 32px;
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                }
                .ticket-band-label {
                  font-size: 10px;
                  letter-spacing: 2px;
                  text-transform: uppercase;
                  color: rgba(255,255,255,.7);
                  margin-bottom: 4px;
                }
                .ticket-band-value {
                  font-size: 28px;
                  font-weight: 700;
                  color: #fff;
                  letter-spacing: 1px;
                }
                .ticket-band-id {
                  font-family: 'DM Mono', monospace;
                  font-size: 13px;
                  color: rgba(255,255,255,.8);
                  letter-spacing: 2px;
                }

                .ticket-body {
                  padding: 28px 32px;
                }

                .data-grid {
                  display: grid;
                  grid-template-columns: 1fr 1fr;
                  gap: 20px;
                  margin-bottom: 24px;
                }
                .data-label {
                  font-family: 'DM Mono', monospace;
                  font-size: 9px;
                  letter-spacing: 2px;
                  text-transform: uppercase;
                  color: #4a5068;
                  margin-bottom: 5px;
                }
                .data-value {
                  font-size: 14px;
                  font-weight: 600;
                  color: #c8cde0;
                }
                .data-value.accent { color: #00C9A7; }

                .perforated {
                  position: relative;
                  margin: 24px 0;
                }
                .perforated::before {
                  content: '';
                  display: block;
                  border-top: 2px dashed #1e2130;
                }
                .perforated .circle-left,
                .perforated .circle-right {
                  position: absolute;
                  top: 50%%;
                  transform: translateY(-50%%);
                  width: 24px;
                  height: 24px;
                  border-radius: 50%%;
                  background: #0b0d14;
                }
                .perforated .circle-left  { left: -44px; }
                .perforated .circle-right { right: -44px; }

                .qr-section {
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  gap: 12px;
                }
                .qr-frame {
                  background: #ffffff;
                  padding: 12px;
                  border-radius: 12px;
                  display: inline-block;
                  box-shadow: 0 0 30px rgba(0, 201, 167, 0.15);
                }
                .qr-frame img {
                  display: block;
                  width: 160px;
                  height: 160px;
                }
                .qr-instructions {
                  font-size: 12px;
                  color: #4a5068;
                  text-align: center;
                  line-height: 1.6;
                  max-width: 280px;
                }
                .qr-instructions strong { color: #00C9A7; }

                .ticket-footer {
                  background: #0e1018;
                  border-top: 1px solid #1e2130;
                  padding: 14px 32px;
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                }
                .ticket-footer-text {
                  font-family: 'DM Mono', monospace;
                  font-size: 10px;
                  color: #2e3347;
                  letter-spacing: 1px;
                }
                .status-badge {
                  font-family: 'DM Mono', monospace;
                  font-size: 10px;
                  letter-spacing: 2px;
                  text-transform: uppercase;
                  color: #00C9A7;
                  background: rgba(0, 201, 167, 0.1);
                  border: 1px solid rgba(0, 201, 167, 0.25);
                  padding: 4px 10px;
                  border-radius: 20px;
                }
              </style>
            </head>
            <body>
              <div class="wrapper">

                <div class="header">
                  <div class="header-logo">Sistema de Parqueo</div>
                  <div class="header-title">Tu reserva está confirmada</div>
                </div>

                <div class="ticket">

                  <div class="ticket-band">
                    <div>
                      <div class="ticket-band-label">Espacio asignado</div>
                      <div class="ticket-band-value">%s</div>
                      <div class="ticket-band-label" style="margin-top:4px">%s — %s</div>
                    </div>
                    <div class="ticket-band-id">%s</div>
                  </div>

                  <div class="ticket-body">

                    <div class="data-grid">
                      <div class="data-item">
                        <div class="data-label">Titular</div>
                        <div class="data-value">%s</div>
                      </div>
                      <div class="data-item">
                        <div class="data-label">Fecha</div>
                        <div class="data-value accent">%s</div>
                      </div>
                      <div class="data-item">
                        <div class="data-label">Entrada</div>
                        <div class="data-value accent">%s</div>
                      </div>
                      <div class="data-item">
                        <div class="data-label">Salida</div>
                        <div class="data-value">%s</div>
                      </div>
                      <div class="data-item">
                        <div class="data-label">Vehículo</div>
                        <div class="data-value">%s</div>
                      </div>
                      <div class="data-item">
                        <div class="data-label">Sede</div>
                        <div class="data-value">%s</div>
                      </div>
                    </div>

                    <div class="perforated">
                      <div class="circle-left"></div>
                      <div class="circle-right"></div>
                    </div>

                    <div class="qr-section">
                      <div class="qr-frame">
                        <img src="data:image/png;base64,%s" alt="Código QR de tu reserva"/>
                      </div>
                      <p class="qr-instructions">
                        Escanea este código al <strong>ingresar</strong> y al <strong>salir</strong> del parqueo.<br/>
                        Válido 5 minutos antes y después de tu hora de entrada.
                      </p>
                    </div>

                  </div>

                  <div class="ticket-footer">
                    <span class="ticket-footer-text">UPB · PARKING SYSTEM</span>
                    <span class="status-badge">✓ CONFIRMADA</span>
                  </div>

                </div>

              </div>
            </body>
            </html>
            """.formatted(
                espacio, zona, sede, reservaId,
                nombre, fecha, horaIni, horaFin,
                reserva.getTipoVehiculo().name(), sede,
                qrBase64
        );
    }
}