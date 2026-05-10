package servidores;

import java.util.concurrent.ConcurrentHashMap;

public class CacheWeb {
    private final ConcurrentHashMap<String, byte[]> almacenamientoMemoria = new ConcurrentHashMap<>();
    private final BackendAWS backend = new BackendAWS();

    public byte[] obtenerVideo(String pelicula) {
        if (almacenamientoMemoria.containsKey(pelicula)) {
            System.out.println("CACHE HIT: " + pelicula + " servido desde memoria.");
            return almacenamientoMemoria.get(pelicula);
        }

        System.out.println("CACHE MISS: " + pelicula + ". Solicitando a Backend AWS...");
        byte[] datos = backend.descargarVideoHTTPS(pelicula);
        almacenamientoMemoria.put(pelicula, datos);

        return datos;
    }
}