package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.EspacioRequest;
import com.upb.TSIS.dto.response.EspacioResponse;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IEspacioService;
import com.upb.TSIS.service.IQrImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EspacioServiceImpl implements IEspacioService {

    private final EspacioRepository espacioRepository;
    private final ZonaRepository    zonaRepository;
    private final IQrImageService   qrImageService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional
    public EspacioResponse crear(EspacioRequest request) {
        Zona zona = zonaRepository.findById(request.getZonaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + request.getZonaId()));

        Espacio espacio = Espacio.builder()
                .zona(zona)
                .codigo(request.getCodigo())
                .tipoVehiculo(request.getTipoVehiculo() != null ? request.getTipoVehiculo() : TipoVehiculo.AUTO)
                .coordenadas(request.getCoordenadas())
                .build();

        // Persiste primero para que @PrePersist asigne el codigoQrFisico
        espacio = espacioRepository.save(espacio);

        // Genera QR con URL completa y lo almacena
        espacio.setQrImagenBase64(generarQrParaEspacio(espacio));
        return toResponse(espacioRepository.save(espacio));
    }

    @Override
    @Transactional(readOnly = true)
    public EspacioResponse obtenerPorId(Integer id) {
        return toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacioResponse> listarPorZona(Integer zonaId) {
        return espacioRepository.findByZona_Id(zonaId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EspacioResponse> listarDisponibles(Integer zonaId, TipoVehiculo tipoVehiculo,
                                                   LocalDateTime inicio, LocalDateTime fin) {
        return espacioRepository.findDisponibles(zonaId, tipoVehiculo, inicio, fin)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public EspacioResponse actualizarEstado(Integer id, EstadoEspacio nuevoEstado) {
        Espacio espacio = buscarOFallar(id);
        espacio.setEstado(nuevoEstado);
        return toResponse(espacioRepository.save(espacio));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        espacioRepository.delete(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] obtenerQrPng(Integer id) {
        Espacio espacio = buscarOFallar(id);
        if (espacio.getQrImagenBase64() == null) {
            // Genera on-the-fly si no está almacenado
            String base64 = generarQrParaEspacio(espacio);
            espacio.setQrImagenBase64(base64);
            espacioRepository.save(espacio);
            return Base64.getDecoder().decode(base64);
        }
        return Base64.getDecoder().decode(espacio.getQrImagenBase64());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generarQrParaEspacio(Espacio espacio) {
        // El QR codifica la URL que abre la app directamente al escanear
        String url = frontendUrl + "/escanear/" + espacio.getCodigoQrFisico();
        return qrImageService.generarBase64(url);
    }

    private Espacio buscarOFallar(Integer id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + id));
    }

    public EspacioResponse toResponse(Espacio e) {
        return EspacioResponse.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .codigoQrFisico(e.getCodigoQrFisico())
                .estado(e.getEstado())
                .tipoVehiculo(e.getTipoVehiculo())
                .coordenadas(e.getCoordenadas())
                .zonaId(e.getZona().getId())
                .zonaNombre(e.getZona().getNombre())
                .build();
    }
}