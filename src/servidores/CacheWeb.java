package servidores;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class CacheWeb {
    private final ConcurrentHashMap<String, byte[]> almacenamiento = new ConcurrentHashMap<>();
    private final BackendAWS backend;
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);

    public CacheWeb(String rutaVideos) {
        this.backend = new BackendAWS(rutaVideos);
    }

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