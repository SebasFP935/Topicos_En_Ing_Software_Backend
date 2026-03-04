package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.ZonaRequest;
import com.upb.TSIS.entity.Sede;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.repository.SedeRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IZonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaServiceImpl implements IZonaService {

    private final ZonaRepository  zonaRepository;
    private final SedeRepository  sedeRepository;

    @Override
    @Transactional
    public Zona crear(ZonaRequest request) {
        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada: " + request.getSedeId()));

        Zona zona = Zona.builder()
                .sede(sede)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .build();
        return zonaRepository.save(zona);
    }

    @Override
    @Transactional(readOnly = true)
    public Zona obtenerPorId(Integer id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Zona> listarPorSede(Integer sedeId) {
        return zonaRepository.findBySede_IdAndActivoTrue(sedeId);
    }

    @Override
    @Transactional
    public Zona actualizar(Integer id, ZonaRequest request) {
        Zona zona = obtenerPorId(id);
        if (request.getNombre()      != null) zona.setNombre(request.getNombre());
        if (request.getDescripcion() != null) zona.setDescripcion(request.getDescripcion());
        if (request.getTipo()        != null) zona.setTipo(request.getTipo());
        return zonaRepository.save(zona);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Zona zona = obtenerPorId(id);
        zona.setActivo(false);
        zonaRepository.save(zona);
    }
}
