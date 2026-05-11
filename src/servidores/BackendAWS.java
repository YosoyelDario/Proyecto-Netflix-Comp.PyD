package servidores;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class BackendAWS {
    private final String rutaVideos;

    public BackendAWS(String rutaVideos) {
        this.rutaVideos = rutaVideos;
    }
    public byte[] descargarVideoHTTPS(String pelicula, int fragmentos) {
        
        try {
            System.out.println("[AWS] Descargando '" + pelicula + "' desde almacenamiento remoto...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

        int tamanioTotal = fragmentos * 5000;
        byte[] datosSimulados = new byte[tamanioTotal];

        for (int i = 0; i < tamanioTotal; i++) {
            datosSimulados[i] = (byte) ((i % 256) ^ (pelicula.hashCode() & 0xFF));
        }

        System.out.println("[AWS] Descarga completada: " + pelicula + " (" + tamanioTotal + " bytes simulados)");
        return datosSimulados;
    }
}