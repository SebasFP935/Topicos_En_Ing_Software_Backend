package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.MapaRequest;
import com.upb.TSIS.dto.response.MapaResponse;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IQrImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MapaServiceImpl {

    private final ZonaRepository    zonaRepository;
    private final EspacioRepository espacioRepository;

    // Agregar en los campos del service:
    private final IQrImageService qrImageService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /api/zonas/{id}/mapa
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * Devuelve el plano completo de la zona:
     * - Elementos decorativos (paredes, pasillos)
     * - Todos los espacios con coordenadas y estado actual
     *
     * Usado tanto por la vista admin (editor) como por la vista
     * usuario (reservar). El estado de cada espacio es en tiempo real.
     */
    @Transactional(readOnly = true)
    public MapaResponse obtenerMapa(Integer zonaId) {
        Zona zona = findZona(zonaId);

        List<MapaResponse.PlanoElementoDto> planoDto = parsePlano(zona.getPlano());
        // Importante: leer desde repository evita devolver colecciones
        // stale en la misma transaccion justo despues de guardar/eliminar.
        List<MapaResponse.EspacioMapaDto>   espaciosDto = espacioRepository.findByZona_Id(zonaId).stream()
                .map(this::toEspacioDto)
                .filter(this::tieneCoordenadasValidas)
                .toList();

        return MapaResponse.builder()
                .zonaId(zona.getId())
                .zonaNombre(zona.getNombre())
                .mapaAncho(zona.getMapaAncho())
                .mapaAlto(zona.getMapaAlto())
                .planoImagen(zona.getPlanoImagen())
                .tieneMapa(zona.getPlano() != null || !espaciosDto.isEmpty())
                .plano(planoDto)
                .espacios(espaciosDto)
                .build();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /api/zonas/{id}/mapa
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    /**
     * El admin guarda el estado completo del editor:
     *
     * 1. Actualiza las dimensiones del canvas en la zona.
     * 2. Reemplaza el plano (paredes y pasillos) de la zona.
     * 3. Para cada espacio en el request:
     *    - Si tiene id â†’ actualiza coordenadas del espacio existente.
     *    - Si no tiene id â†’ crea un espacio nuevo en la BD.
     * 4. Elimina espacios que ya no estÃ¡n en el mapa (fueron borrados
     *    por el admin en el editor).
     *
     * Todo en una sola transacciÃ³n.
     */
    @Transactional
    public MapaResponse guardarMapa(Integer zonaId, MapaRequest req) {
        Zona zona = findZona(zonaId);

        // 1. Canvas dimensions
        zona.setMapaAncho(req.getMapaAncho());
        zona.setMapaAlto(req.getMapaAlto());
        zona.setPlanoImagen(normalizeImage(req.getPlanoImagen()));

        // 2. Plano (paredes y pasillos) â€” lista de Maps que Hibernate
        //    serializa directamente a JSONB
        zona.setPlano(req.getPlano().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", e.getType());
                    m.put("x", e.getX()); m.put("y", e.getY());
                    m.put("w", e.getW()); m.put("h", e.getH());
                    return m;
                })
                .toList());

        zonaRepository.save(zona);

        // 3. Espacios: separar nuevos de existentes
        Set<Integer> idsEnRequest = new HashSet<>();
        Set<String> codigosEnRequest = new HashSet<>();

        for (MapaRequest.EspacioMapaDto dto : req.getEspacios()) {

            Map<String, Object> coords = buildCoords(dto);

            if (dto.getId() != null) {
                // Espacio existente â†’ solo actualizar coordenadas
                Espacio esp = espacioRepository.findById(dto.getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Espacio no encontrado: " + dto.getId()));
                validarPertenencia(esp, zonaId);
                esp.setCoordenadas(coords);
                if (dto.getTipoVehiculo() != null && !dto.getTipoVehiculo().isBlank()) {
                    esp.setTipoVehiculo(parseTipo(dto.getTipoVehiculo()));
                }
                espacioRepository.save(esp);
                idsEnRequest.add(esp.getId());
                codigosEnRequest.add(esp.getCodigo().trim().toUpperCase());

            } else {
                // Espacio nuevo â†’ crear
                if (dto.getCodigo() == null || dto.getCodigo().isBlank())
                    throw new ReglaNegocioException("El cÃ³digo es obligatorio para espacios nuevos.");

                if (dto.getTipoVehiculo() == null)
                    throw new ReglaNegocioException("El tipo de vehÃ­culo es obligatorio para espacios nuevos.");

                String codigoNormalizado = dto.getCodigo().trim().toUpperCase();
                if (!codigosEnRequest.add(codigoNormalizado)) {
                    throw new ReglaNegocioException("El cÃ³digo '" + codigoNormalizado + "' estÃ¡ repetido en el mapa.");
                }

                // Si llega sin id pero ya existe en la misma zona, actualizamos para
                // soportar payloads donde el frontend no enviÃ³ el id persistido.
                Optional<Espacio> existenteMismaZona =
                        espacioRepository.findByZona_IdAndCodigoIgnoreCase(zonaId, codigoNormalizado);

                if (existenteMismaZona.isPresent()) {
                    Espacio esp = existenteMismaZona.get();
                    esp.setCoordenadas(coords);
                    esp.setTipoVehiculo(parseTipo(dto.getTipoVehiculo()));
                    espacioRepository.save(esp);
                    idsEnRequest.add(esp.getId());
                    continue;
                }

                // El cÃ³digo es Ãºnico global en la tabla espacios.
                Optional<Espacio> existenteGlobal =
                        espacioRepository.findByCodigoIgnoreCase(codigoNormalizado);
                if (existenteGlobal.isPresent()) {
                    throw new ReglaNegocioException(
                            "El cÃ³digo '" + codigoNormalizado + "' ya existe en otra zona. Usa un cÃ³digo distinto.");
                }

                Espacio nuevo = Espacio.builder()
                        .zona(zona)
                        .codigo(codigoNormalizado)
                        .tipoVehiculo(parseTipo(dto.getTipoVehiculo()))
                        .estado(EstadoEspacio.DISPONIBLE)
                        .coordenadas(coords)
                        .build();

                Espacio guardado = espacioRepository.save(nuevo);

                // Generar y guardar QR físico
                if (guardado.getCodigoQrFisico() != null && guardado.getQrImagenBase64() == null) {
                    String url = frontendUrl + "/escanear/" + guardado.getCodigoQrFisico();
                    guardado.setQrImagenBase64(qrImageService.generarBase64(url));
                    guardado = espacioRepository.save(guardado);
                }

                idsEnRequest.add(guardado.getId());
            }
        }

        // 4. Eliminar espacios que el admin borrÃ³ del editor
        //    Solo eliminamos si NO tienen reservas activas.
        List<Espacio> espaciosActuales = espacioRepository.findByZona_Id(zonaId);
        for (Espacio esp : espaciosActuales) {
            if (!idsEnRequest.contains(esp.getId())) {
                boolean tieneReservaActiva = esp.getReservas() != null &&
                        esp.getReservas().stream()
                                .anyMatch(r -> r.getEstado() != null &&
                                        !r.getEstado().name().equals("CANCELADA") &&
                                        !r.getEstado().name().equals("NO_SHOW") &&
                                        !r.getEstado().name().equals("COMPLETADA"));

                if (tieneReservaActiva) {
                    // No eliminar, solo quitar del mapa (coordenadas null)
                    esp.setCoordenadas(null);
                    espacioRepository.save(esp);
                } else {
                    espacioRepository.delete(esp);
                }
            }
        }

        // Sincroniza cambios de espacios antes de reconstruir la respuesta.
        espacioRepository.flush();

        return obtenerMapa(zonaId);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Zona findZona(Integer id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + id));
    }

    private void validarPertenencia(Espacio esp, Integer zonaId) {
        if (!esp.getZona().getId().equals(zonaId))
            throw new ReglaNegocioException(
                    "El espacio " + esp.getId() + " no pertenece a la zona " + zonaId);
    }

    private Map<String, Object> buildCoords(MapaRequest.EspacioMapaDto dto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", dto.getX()); m.put("y", dto.getY());
        m.put("w", dto.getW()); m.put("h", dto.getH());
        m.put("angulo", dto.getAngulo() != null ? dto.getAngulo() : 0d);
        return m;
    }

    private TipoVehiculo parseTipo(String tipo) {
        try {
            String normalizado = tipo.toUpperCase().trim();
            if ("DISCAPACIDAD".equals(normalizado)) normalizado = "DISCAPACITADO";
            if ("BICICLETA".equals(normalizado)) normalizado = "MOTO";
            return TipoVehiculo.valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de vehiculo invalido: " + tipo);
        }
    }

    private String normalizeImage(String imageDataUrl) {
        if (imageDataUrl == null) return null;
        String value = imageDataUrl.trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Convierte el campo plano (Object/List que Hibernate deserializa
     * del JSONB) en la lista de DTOs tipada.
     */
    @SuppressWarnings("unchecked")
    private List<MapaResponse.PlanoElementoDto> parsePlano(Object raw) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> list)) return List.of();

        return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    return MapaResponse.PlanoElementoDto.builder()
                            .type(str(m, "type"))
                            .x(num(m, "x")).y(num(m, "y"))
                            .w(num(m, "w")).h(num(m, "h"))
                            .build();
                })
                .toList();
    }

    private MapaResponse.EspacioMapaDto toEspacioDto(Espacio e) {
        Double x = null, y = null, w = null, h = null, angulo = null;

        if (e.getCoordenadas() instanceof Map<?, ?> c) {
            x = num((Map<String,Object>) c, "x");
            y = num((Map<String,Object>) c, "y");
            w = num((Map<String,Object>) c, "w");
            h = num((Map<String,Object>) c, "h");
            angulo = num((Map<String,Object>) c, "angulo");
        }

        return MapaResponse.EspacioMapaDto.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .tipoVehiculo(e.getTipoVehiculo().name())
                .estado(e.getEstado().name())
                .x(x).y(y).w(w).h(h)
                .angulo(angulo != null ? angulo : 0d)
                .build();
    }

    private boolean tieneCoordenadasValidas(MapaResponse.EspacioMapaDto dto) {
        return dto.getX() != null && dto.getY() != null
                && dto.getW() != null && dto.getH() != null;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }

    private Double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }
}

