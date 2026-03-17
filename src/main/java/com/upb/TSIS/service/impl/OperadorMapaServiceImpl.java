package com.upb.TSIS.service.impl;

import com.upb.TSIS.dto.request.AutoLayoutRequest;
import com.upb.TSIS.dto.request.EspacioOperadorRequest;
import com.upb.TSIS.dto.request.ImagenFondoRequest;
import com.upb.TSIS.dto.request.MapaRequest;
import com.upb.TSIS.dto.request.MoverEspacioRequest;
import com.upb.TSIS.dto.response.OperadorMapaResponse;
import com.upb.TSIS.entity.Espacio;
import com.upb.TSIS.entity.Zona;
import com.upb.TSIS.entity.enums.EstadoEspacio;
import com.upb.TSIS.entity.enums.TipoVehiculo;
import com.upb.TSIS.exception.RecursoNoEncontradoException;
import com.upb.TSIS.exception.ReglaNegocioException;
import com.upb.TSIS.repository.EspacioRepository;
import com.upb.TSIS.repository.ZonaRepository;
import com.upb.TSIS.service.IOperadorMapaService;
import com.upb.TSIS.service.IQrImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperadorMapaServiceImpl implements IOperadorMapaService {

    private final ZonaRepository    zonaRepository;
    private final EspacioRepository espacioRepository;
    private final IQrImageService   qrImageService;

    // ─────────────────────────────────────────────────────────────
    // IMAGEN DE FONDO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void subirImagenFondo(Integer zonaId, ImagenFondoRequest req) {
        Zona zona = findZona(zonaId);
        zona.setImagenFondo(req.getImagenBase64());
        if (req.getImageWidth()  != null) zona.setMapaAncho(req.getImageWidth());
        if (req.getImageHeight() != null) zona.setMapaAlto(req.getImageHeight());
        zonaRepository.save(zona);
    }

    // ─────────────────────────────────────────────────────────────
    // AUTO-LAYOUT
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OperadorMapaResponse generarEspaciosAutomatico(Integer zonaId, AutoLayoutRequest req) {
        Zona zona = findZona(zonaId);

        int canvasAncho = zona.getMapaAncho() != null ? zona.getMapaAncho() : 1200;
        int canvasAlto  = zona.getMapaAlto()  != null ? zona.getMapaAlto()  : 700;

        double espW = req.getEspacioAncho();
        double espH = req.getEspacioAlto();
        double spH  = req.getEspaciadoH();
        double spV  = req.getEspaciadoV();
        double mX   = req.getMargenX();
        double mY   = req.getMargenY();

        int cols = req.getColumnas() != null
                ? req.getColumnas()
                : (int) Math.floor((canvasAncho - 2 * mX + spH) / (espW + spH));
        int rows = req.getFilas() != null
                ? req.getFilas()
                : (int) Math.floor((canvasAlto - 2 * mY + spV) / (espH + spV));

        cols = Math.max(1, cols);
        rows = Math.max(1, rows);

        // Cargar espacios existentes UNA sola vez — evita N+1 dentro del loop
        List<Espacio> existentes = espacioRepository.findByZona_Id(zonaId);

        if (req.isReemplazar()) {
            for (Espacio e : existentes) {
                if (!tieneReservaActiva(e)) {
                    espacioRepository.delete(e);
                } else {
                    log.warn("Espacio {} tiene reservas activas — omitido en auto-layout", e.getId());
                }
            }
            espacioRepository.flush();
            // Tras limpiar, el set de códigos usados queda vacío para los activos con reserva
            existentes = espacioRepository.findByZona_Id(zonaId);
        }

        // Set de códigos ya existentes para evitar duplicados sin ir a BD en cada iteración
        Set<String> codigosExistentes = existentes.stream()
                .map(e -> e.getCodigo().toUpperCase())
                .collect(Collectors.toSet());

        TipoVehiculo tipo = parseTipo(req.getTipoVehiculo());

        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + (r % 26));  // A-Z, luego A otra vez (máx 26 filas con letra única)
            for (int c = 0; c < cols; c++) {
                String codigo = rowChar + "-" + String.format("%02d", c + 1);

                if (codigosExistentes.contains(codigo)) {
                    log.debug("Código {} ya existe — se omite", codigo);
                    continue;
                }

                double x = mX + c * (espW + spH);
                double y = mY + r * (espH + spV);

                Map<String, Object> coords = new LinkedHashMap<>();
                coords.put("x", x); coords.put("y", y);
                coords.put("w", espW); coords.put("h", espH);

                Espacio nuevo = Espacio.builder()
                        .zona(zona)
                        .codigo(codigo)
                        .tipoVehiculo(tipo)
                        .estado(EstadoEspacio.DISPONIBLE)
                        .coordenadas(coords)
                        .build();

                espacioRepository.save(nuevo);
                codigosExistentes.add(codigo); // actualizar el set in-memory
            }
        }

        return obtenerMapa(zonaId);
    }

    // ─────────────────────────────────────────────────────────────
    // OBTENER MAPA
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OperadorMapaResponse obtenerMapa(Integer zonaId) {
        Zona zona = findZona(zonaId);

        List<OperadorMapaResponse.PlanoElementoDto> planoDto = parsePlano(zona.getPlano());

        List<OperadorMapaResponse.EspacioOperadorDto> espaciosDto =
                espacioRepository.findByZona_Id(zonaId).stream()
                        .map(this::toEspacioDto)
                        .toList();

        return OperadorMapaResponse.builder()
                .zonaId(zona.getId())
                .zonaNombre(zona.getNombre())
                .mapaAncho(zona.getMapaAncho())
                .mapaAlto(zona.getMapaAlto())
                .imagenFondo(zona.getImagenFondo())
                .tieneMapa(zona.getPlano() != null || !espaciosDto.isEmpty() || zona.getImagenFondo() != null)
                .plano(planoDto)
                .espacios(espaciosDto)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // GUARDAR MAPA COMPLETO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OperadorMapaResponse guardarMapa(Integer zonaId, MapaRequest req) {
        Zona zona = findZona(zonaId);

        zona.setMapaAncho(req.getMapaAncho());
        zona.setMapaAlto(req.getMapaAlto());

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

        Set<Integer> idsEnRequest = new HashSet<>();

        for (MapaRequest.EspacioMapaDto dto : req.getEspacios()) {
            Map<String, Object> coords = buildCoords(dto);

            if (dto.getId() != null) {
                Espacio esp = espacioRepository.findById(dto.getId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Espacio no encontrado: " + dto.getId()));
                validarPertenencia(esp, zonaId);
                esp.setCoordenadas(coords);
                espacioRepository.save(esp);
                idsEnRequest.add(esp.getId());
            } else {
                if (dto.getCodigo() == null || dto.getCodigo().isBlank())
                    throw new ReglaNegocioException("codigo obligatorio para espacios nuevos.");
                if (dto.getTipoVehiculo() == null)
                    throw new ReglaNegocioException("tipoVehiculo obligatorio para espacios nuevos.");

                Espacio nuevo = Espacio.builder()
                        .zona(zona)
                        .codigo(dto.getCodigo().trim().toUpperCase())
                        .tipoVehiculo(parseTipo(dto.getTipoVehiculo()))
                        .estado(EstadoEspacio.DISPONIBLE)
                        .coordenadas(coords)
                        .build();

                idsEnRequest.add(espacioRepository.save(nuevo).getId());
            }
        }

        // Eliminar los que ya no están en el request
        for (Espacio esp : espacioRepository.findByZona_Id(zonaId)) {
            if (!idsEnRequest.contains(esp.getId())) {
                if (tieneReservaActiva(esp)) {
                    esp.setCoordenadas(null);
                    espacioRepository.save(esp);
                } else {
                    espacioRepository.delete(esp);
                }
            }
        }

        return obtenerMapa(zonaId);
    }

    // ─────────────────────────────────────────────────────────────
    // ESPACIO INDIVIDUAL
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public OperadorMapaResponse.EspacioOperadorDto agregarEspacio(Integer zonaId, EspacioOperadorRequest req) {
        Zona zona = findZona(zonaId);

        Map<String, Object> coords = new LinkedHashMap<>();
        coords.put("x", req.getX()); coords.put("y", req.getY());
        coords.put("w", req.getW()); coords.put("h", req.getH());

        Espacio nuevo = Espacio.builder()
                .zona(zona)
                .codigo(req.getCodigo().trim().toUpperCase())
                .tipoVehiculo(parseTipo(req.getTipoVehiculo()))
                .estado(EstadoEspacio.DISPONIBLE)
                .coordenadas(coords)
                .build();

        return toEspacioDto(espacioRepository.save(nuevo));
    }

    @Override
    @Transactional
    public void eliminarEspacio(Integer zonaId, Integer espacioId) {
        Espacio esp = findEspacio(espacioId);
        validarPertenencia(esp, zonaId);

        if (tieneReservaActiva(esp)) {
            throw new ReglaNegocioException(
                    "El espacio " + esp.getCodigo() + " tiene reservas activas y no puede eliminarse.");
        }

        espacioRepository.delete(esp);
    }

    @Override
    @Transactional
    public OperadorMapaResponse.EspacioOperadorDto moverEspacio(
            Integer zonaId, Integer espacioId, MoverEspacioRequest req) {

        Espacio esp = findEspacio(espacioId);
        validarPertenencia(esp, zonaId);

        @SuppressWarnings("unchecked")
        Map<String, Object> coords = esp.getCoordenadas() instanceof Map<?, ?> c
                ? new LinkedHashMap<>((Map<String, Object>) c)
                : new LinkedHashMap<>();

        coords.put("x", req.getX());
        coords.put("y", req.getY());
        if (req.getW() != null) coords.put("w", req.getW());
        if (req.getH() != null) coords.put("h", req.getH());

        esp.setCoordenadas(coords);
        return toEspacioDto(espacioRepository.save(esp));
    }

    // ─────────────────────────────────────────────────────────────
    // QR IMAGE DEL ESPACIO FÍSICO
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public String obtenerQrImagenEspacio(Integer espacioId) {
        Espacio esp = findEspacio(espacioId);
        return qrImageService.generarDataUrl(esp.getCodigoQr());
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private Zona findZona(Integer id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Zona no encontrada: " + id));
    }

    private Espacio findEspacio(Integer id) {
        return espacioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Espacio no encontrado: " + id));
    }

    private void validarPertenencia(Espacio esp, Integer zonaId) {
        if (!esp.getZona().getId().equals(zonaId))
            throw new ReglaNegocioException(
                    "El espacio " + esp.getId() + " no pertenece a la zona " + zonaId);
    }

    private boolean tieneReservaActiva(Espacio esp) {
        return esp.getReservas() != null && esp.getReservas().stream()
                .anyMatch(r -> r.getEstado() != null &&
                        !r.getEstado().name().equals("CANCELADA") &&
                        !r.getEstado().name().equals("NO_SHOW") &&
                        !r.getEstado().name().equals("COMPLETADA"));
    }

    private TipoVehiculo parseTipo(String tipo) {
        try {
            return TipoVehiculo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException("Tipo de vehículo inválido: " + tipo);
        }
    }

    private Map<String, Object> buildCoords(MapaRequest.EspacioMapaDto dto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", dto.getX()); m.put("y", dto.getY());
        m.put("w", dto.getW()); m.put("h", dto.getH());
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<OperadorMapaResponse.PlanoElementoDto> parsePlano(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> {
                    Map<String, Object> m = (Map<String, Object>) item;
                    return OperadorMapaResponse.PlanoElementoDto.builder()
                            .type(m.get("type") != null ? m.get("type").toString() : null)
                            .x(numVal(m, "x")).y(numVal(m, "y"))
                            .w(numVal(m, "w")).h(numVal(m, "h"))
                            .build();
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private OperadorMapaResponse.EspacioOperadorDto toEspacioDto(Espacio e) {
        Double x = null, y = null, w = null, h = null;
        if (e.getCoordenadas() instanceof Map<?, ?> c) {
            Map<String, Object> m = (Map<String, Object>) c;
            x = numVal(m, "x"); y = numVal(m, "y");
            w = numVal(m, "w"); h = numVal(m, "h");
        }
        return OperadorMapaResponse.EspacioOperadorDto.builder()
                .id(e.getId())
                .codigo(e.getCodigo())
                .tipoVehiculo(e.getTipoVehiculo().name())
                .estado(e.getEstado().name())
                .x(x).y(y).w(w).h(h)
                .codigoQr(e.getCodigoQr())
                .build();
    }

    private Double numVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }
}