package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.ListaEsperaRequest;
import com.upb.TSIS.entity.ListaEspera;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspera;
import com.upb.TSIS.entity.enums.TipoNotificacion;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.ListaEsperaRepository;
import com.upb.TSIS.repository.UsuarioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IListaEsperaService;
import com.upb.TSIS.service.INotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListaEsperaServiceImpl implements IListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final UsuarioRepository     usuarioRepository;
    private final ZonaRepository        zonaRepository;
    private final INotificacionService  notificacionService;

    @Override
    @Transactional
    public ListaEspera agregar(Integer usuarioId, ListaEsperaRequest request) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + usuarioId));

        if (listaEsperaRepository.existsByUsuario_IdAndEstado(usuarioId, EstadoEspera.ESPERANDO)) {
            throw new ReglaNegocioException("Ya tienes una solicitud activa en lista de espera.");
        }

        Zona zona = null;
        if (request.getZonaPreferidaId() != null) {
            zona = zonaRepository.findById(request.getZonaPreferidaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + request.getZonaPreferidaId()));
        }

        ListaEspera entrada = ListaEspera.builder()
                .usuario(usuario)
                .zonaPreferida(zona)
                .fechaDeseadaInicio(request.getFechaDeseadaInicio())
                .fechaDeseadaFin(request.getFechaDeseadaFin())
                .build();

        return listaEsperaRepository.save(entrada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListaEspera> listarPorUsuario(Integer usuarioId) {
        return listaEsperaRepository.findByUsuario_Id(usuarioId);
    }

    @Override
    @Transactional
    public void cancelar(Integer listaEsperaId, Integer usuarioId) {
        ListaEspera entrada = listaEsperaRepository.findById(listaEsperaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrada no encontrada: " + listaEsperaId));

        if (!entrada.getUsuario().getId().equals(usuarioId)) {
            throw new ReglaNegocioException("No puedes cancelar una solicitud que no es tuya.");
        }

        entrada.setEstado(EstadoEspera.EXPIRADO);
        listaEsperaRepository.save(entrada);
    }

    @Override
    @Transactional
    public void notificarSiguiente(Integer zonaId) {
        List<ListaEspera> espera = listaEsperaRepository
                .findByZonaPreferida_IdAndEstadoOrderByFechaSolicitudAsc(zonaId, EstadoEspera.ESPERANDO);

        if (!espera.isEmpty()) {
            ListaEspera siguiente = espera.get(0);
            siguiente.setEstado(EstadoEspera.NOTIFICADO);
            listaEsperaRepository.save(siguiente);

            notificacionService.enviar(
                    siguiente.getUsuario(),
                    TipoNotificacion.PUSH,
                    "¡Hay un espacio disponible!",
                    "Se liberó un espacio en la zona que solicitaste. Ingresa a reservar ahora."
            );
        }
    }
}
