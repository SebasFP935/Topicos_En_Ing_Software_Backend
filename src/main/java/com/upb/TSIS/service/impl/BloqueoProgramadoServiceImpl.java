package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.BloqueoRequest;
import com.upb.TSIS.entity.BloqueoProgramado;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.BloqueoProgramadoRepository;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IBloqueoProgramadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueoProgramadoServiceImpl implements IBloqueoProgramadoService {

    private final BloqueoProgramadoRepository bloqueoRepository;
    private final EspacioRepository           espacioRepository;
    private final ZonaRepository              zonaRepository;
    private final UsuarioRepository           usuarioRepository;

    @Override
    @Transactional
    public BloqueoProgramado crear(Integer adminId, BloqueoRequest request) {
        // Validar que venga solo uno (espacio o zona)
        boolean tieneEspacio = request.getEspacioId() != null;
        boolean tieneZona    = request.getZonaId() != null;
        if (tieneEspacio == tieneZona) {
            throw new ReglaNegocioException("Debe especificar un espacio OR una zona para el bloqueo, no ambos.");
        }

        var admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Admin no encontrado: " + adminId));

        Espacio espacio = null;
        Zona    zona    = null;

        if (tieneEspacio) {
            espacio = espacioRepository.findById(request.getEspacioId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + request.getEspacioId()));
            espacio.setEstado(EstadoEspacio.BLOQUEADO);
            espacioRepository.save(espacio);
        } else {
            zona = zonaRepository.findById(request.getZonaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + request.getZonaId()));
            // Bloquear todos los espacios de la zona
            List<Espacio> espacios = espacioRepository.findByZona_Id(zona.getId());
            for (Espacio e : espacios) {
                e.setEstado(EstadoEspacio.BLOQUEADO);
            }
            espacioRepository.saveAll(espacios);
        }

        BloqueoProgramado bloqueo = BloqueoProgramado.builder()
                .espacio(espacio)
                .zona(zona)
                .admin(admin)
                .motivo(request.getMotivo())
                .fechaInicioBloqueo(request.getFechaInicioBloqueo())
                .fechaFinBloqueo(request.getFechaFinBloqueo())
                .build();

        return bloqueoRepository.save(bloqueo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoProgramado> listarPorEspacio(Integer espacioId) {
        return bloqueoRepository.findByEspacio_Id(espacioId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BloqueoProgramado> listarPorZona(Integer zonaId) {
        return bloqueoRepository.findByZona_Id(zonaId);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        BloqueoProgramado bloqueo = bloqueoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloqueo no encontrado: " + id));

        // Restaurar estado de los espacios afectados
        if (bloqueo.getEspacio() != null) {
            bloqueo.getEspacio().setEstado(EstadoEspacio.DISPONIBLE);
            espacioRepository.save(bloqueo.getEspacio());
        } else if (bloqueo.getZona() != null) {
            List<Espacio> espacios = espacioRepository.findByZona_Id(bloqueo.getZona().getId());
            for (Espacio e : espacios) {
                e.setEstado(EstadoEspacio.DISPONIBLE);
            }
            espacioRepository.saveAll(espacios);
        }

        bloqueoRepository.delete(bloqueo);
    }
}
