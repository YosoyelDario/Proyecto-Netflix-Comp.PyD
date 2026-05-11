package servidores;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cache Web del Servidor de Streaming.
 *
 * Almacena en memoria los datos de video descargados del Backend AWS
 * para evitar descargas repetidas (patrón Cache-Aside).
 *
 * Simula el comportamiento de un CDN/Open Connect Appliance:
 * - CACHE HIT: video ya descargado, se sirve desde memoria (rápido)
 * - CACHE MISS: video no en memoria, se descarga del backend (lento)
 */
public class CacheWeb {
    private final ConcurrentHashMap<String, byte[]> almacenamiento = new ConcurrentHashMap<>();
    private final BackendAWS backend;
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);

    public CacheWeb(String rutaVideos) {
        this.backend = new BackendAWS(rutaVideos);
    }

    /**
     * Obtiene los datos de video. Si están en caché los sirve directo;
     * si no, los descarga del backend y los cachea.
     */
    public byte[] obtenerVideo(String pelicula, int fragmentos) {
        if (almacenamiento.containsKey(pelicula)) {
            hits.incrementAndGet();
            System.out.println("[CACHE] HIT: '" + pelicula + "' servido desde memoria. (Hits: " + hits + " / Misses: " + misses + ")");
            return almacenamiento.computeIfAbsent(pelicula, key -> backend.descargarVideoHTTPS(key, fragmentos));
        }

        misses.incrementAndGet();
        System.out.println("[CACHE] MISS: '" + pelicula + "'. Descargando desde Backend AWS...");

        byte[] datos = backend.descargarVideoHTTPS(pelicula, fragmentos);
        almacenamiento.put(pelicula, datos);

        System.out.println("[CACHE] Almacenado en caché: '" + pelicula + "' (" + datos.length + " bytes). (Hits: " + hits + " / Misses: " + misses + ")");
        return datos;
    }
}