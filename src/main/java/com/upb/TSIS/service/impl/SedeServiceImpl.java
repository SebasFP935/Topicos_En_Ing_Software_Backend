package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.SedeRequest;
import com.upb.TSIS.entity.Sede;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.repository.SedeRepository;
import com.upb.TSIS.service.ISedeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeServiceImpl implements ISedeService {

    private final SedeRepository sedeRepository;

    @Override
    @Transactional
    public Sede crear(SedeRequest request) {
        Sede sede = Sede.builder()
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .latitud(request.getLatitud())
                .longitud(request.getLongitud())
                .build();
        return sedeRepository.save(sede);
    }

    @Override
    @Transactional(readOnly = true)
    public Sede obtenerPorId(Integer id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sede> listarActivas() {
        return sedeRepository.findByActivoTrue();
    }

    @Override
    @Transactional
    public Sede actualizar(Integer id, SedeRequest request) {
        Sede sede = obtenerPorId(id);
        if (request.getNombre()    != null) sede.setNombre(request.getNombre());
        if (request.getDireccion() != null) sede.setDireccion(request.getDireccion());
        if (request.getLatitud()   != null) sede.setLatitud(request.getLatitud());
        if (request.getLongitud()  != null) sede.setLongitud(request.getLongitud());
        return sedeRepository.save(sede);
    }

    @Override
    @Transactional
    public void desactivar(Integer id) {
        Sede sede = obtenerPorId(id);
        sede.setActivo(false);
        sedeRepository.save(sede);
    }
}
