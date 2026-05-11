package servidores;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Backend AWS - Simula almacenamiento remoto de videos (S3).
 *
 * Genera datos binarios simulados por película. Si existe un archivo
 * real en data/videos/, lo lee; si no, genera bytes aleatorios simulando
 * la descarga desde un almacenamiento remoto con latencia de red.
 */
public class BackendAWS {
    private final String rutaVideos;

    public BackendAWS(String rutaVideos) {
        this.rutaVideos = rutaVideos;
    }

    /**
     * Simula descarga HTTPS desde almacenamiento remoto.
     * Incluye latencia artificial para simular la red.
     */
    public byte[] descargarVideoHTTPS(String pelicula, int fragmentos) {
        // Simular latencia de red (descarga desde AWS/S3)
        try {
            System.out.println("[AWS] Descargando '" + pelicula + "' desde almacenamiento remoto...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Intentar leer archivo real primero
        String nombreArchivo = pelicula.toLowerCase().replaceAll("\\s+", "_") + ".bin";
        File archivo = new File(rutaVideos, nombreArchivo);

        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo)) {
                byte[] datos = fis.readAllBytes();
                System.out.println("[AWS] Archivo real cargado: " + nombreArchivo + " (" + datos.length + " bytes)");
                return datos;
            } catch (IOException e) {
                System.err.println("[AWS] Error leyendo archivo: " + e.getMessage());
            }
        }

        // Si no hay archivo real, generar datos simulados
        // Cada fragmento tiene un tamaño simulado de ~5KB
        int tamanioTotal = fragmentos * 5000;
        byte[] datosSimulados = new byte[tamanioTotal];

        // Llenar con datos no-cero para simular contenido real
        for (int i = 0; i < tamanioTotal; i++) {
            datosSimulados[i] = (byte) ((i % 256) ^ (pelicula.hashCode() & 0xFF));
        }

        System.out.println("[AWS] Descarga completada: " + pelicula + " (" + tamanioTotal + " bytes simulados)");
        return datosSimulados;
    }
}