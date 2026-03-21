package com.upb.TSIS.service.impl;

import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.service.IQrImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final IQrImageService   qrImageService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void completarQrFaltantes() {
        List<Espacio> todos = espacioRepository.findAll();
        int countUuid = 0, countImg = 0;

        for (Espacio espacio : todos) {
            boolean modificado = false;

            if (espacio.getCodigoQrFisico() == null || espacio.getCodigoQrFisico().isBlank()) {
                espacio.setCodigoQrFisico(UUID.randomUUID().toString());
                modificado = true;
                countUuid++;
            }

            if (espacio.getQrImagenBase64() == null) {
                String url = frontendUrl + "/escanear/" + espacio.getCodigoQrFisico();
                espacio.setQrImagenBase64(qrImageService.generarBase64(url));
                modificado = true;
                countImg++;
            }

            if (modificado) espacioRepository.save(espacio);
        }

        if (countUuid > 0) log.info("Bootstrap QR: {} UUIDs asignados.", countUuid);
        if (countImg  > 0) log.info("Bootstrap QR: {} imágenes generadas.", countImg);
    }
}