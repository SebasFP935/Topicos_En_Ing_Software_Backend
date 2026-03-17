package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.EspacioRequest;
import com.upb.TSIS.dto.response.EspacioQrResponse;
import com.upb.TSIS.dto.response.EspacioResponse;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IEspacioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EspacioServiceImpl implements IEspacioService {

    private final EspacioRepository espacioRepository;
    private final ZonaRepository    zonaRepository;

    @Override
    @Transactional
    public EspacioResponse crear(EspacioRequest request) {
        Zona zona = zonaRepository.findById(request.getZonaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + request.getZonaId()));

        Espacio espacio = Espacio.builder()
                .zona(zona)
                .codigo(request.getCodigo())
                .codigoQr(normalizarCodigoQr(request.getCodigoQr()))
                .tipoVehiculo(request.getTipoVehiculo() != null ? request.getTipoVehiculo() : TipoVehiculo.AUTO)
                .coordenadas(request.getCoordenadas())
                .build();

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
    public EspacioQrResponse regenerarCodigoQr(Integer id) {
        Espacio espacio = buscarOFallar(id);
        espacio.regenerarCodigoQr();
        return toQrResponse(espacioRepository.save(espacio));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        espacioRepository.delete(buscarOFallar(id));
    }

    private String normalizarCodigoQr(String codigoQr) {
        if (codigoQr == null || codigoQr.isBlank()) {
            return null;
        }
        return codigoQr.trim();
    }

    private Espacio buscarOFallar(Integer id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + id));
    }

    public EspacioResponse toResponse(Espacio e) {
        return EspacioResponse.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .estado(e.getEstado())
                .tipoVehiculo(e.getTipoVehiculo())
                .coordenadas(e.getCoordenadas())
                .zonaId(e.getZona().getId())
                .zonaNombre(e.getZona().getNombre())
                .build();
    }

    private EspacioQrResponse toQrResponse(Espacio e) {
        return EspacioQrResponse.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .codigoQr(e.getCodigoQr())
                .build();
    }
}
