package com.upb.TSIS.service.impl;

import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.repository.EspacioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EspacioQrBootstrapService {

    private final EspacioRepository espacioRepository;

    /**
     * Backfill para espacios antiguos que aún no tienen QR físico generado.
     * Se ejecuta una vez al iniciar la app.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void completarCodigosQrFisicosFaltantes() {
        List<Espacio> espaciosSinQr = espacioRepository.findByCodigoQrFisicoIsNull();
        if (espaciosSinQr.isEmpty()) {
            return;
        }

        for (Espacio espacio : espaciosSinQr) {
            espacio.setCodigoQrFisico(UUID.randomUUID().toString());
        }
        espacioRepository.saveAll(espaciosSinQr);

        log.info("Se asignaron {} códigos QR físicos faltantes a espacios existentes.", espaciosSinQr.size());
    }
}
