package com.upb.TSIS.service;

import com.upb.TSIS.dto.request.AutoLayoutRequest;
import com.upb.TSIS.dto.request.EspacioOperadorRequest;
import com.upb.TSIS.dto.request.ImagenFondoRequest;
import com.upb.TSIS.dto.request.MoverEspacioRequest;
import com.upb.TSIS.dto.response.OperadorMapaResponse;

public interface IOperadorMapaService {

    /** Guarda la imagen de fondo (base64 data URL) en la zona y actualiza dimensiones del canvas si se proveen. */
    void subirImagenFondo(Integer zonaId, ImagenFondoRequest req);

    /**
     * Genera automáticamente espacios en grilla sobre la zona.
     * Si reemplazar=true elimina los espacios sin reservas activas primero.
     */
    OperadorMapaResponse generarEspaciosAutomatico(Integer zonaId, AutoLayoutRequest req);

    /** Retorna el mapa completo de la zona incluyendo imagenFondo y codigoQr por espacio. */
    OperadorMapaResponse obtenerMapa(Integer zonaId);

    /**
     * Guarda el estado completo del editor (plano + espacios).
     * Misma semántica que MapaServiceImpl.guardarMapa pero accesible para OPERADOR.
     */
    OperadorMapaResponse guardarMapa(Integer zonaId, com.upb.TSIS.dto.request.MapaRequest req);

    /** Añade un único espacio nuevo a la zona. */
    OperadorMapaResponse.EspacioOperadorDto agregarEspacio(Integer zonaId, EspacioOperadorRequest req);

    /**
     * Elimina un espacio.
     * Si tiene reservas activas lanza ReglaNegocioException.
     */
    void eliminarEspacio(Integer zonaId, Integer espacioId);

    /** Actualiza posición (y opcionalmente tamaño) de un espacio. */
    OperadorMapaResponse.EspacioOperadorDto moverEspacio(Integer zonaId, Integer espacioId, MoverEspacioRequest req);

    /**
     * Retorna la imagen QR del espacio como data URL PNG base64.
     * El QR codifica el codigoQr (UUID permanente del slot físico).
     */
    String obtenerQrImagenEspacio(Integer espacioId);
}