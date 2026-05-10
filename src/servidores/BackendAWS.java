package servidores;

public class BackendAWS {

    public byte[] descargarVideoHTTPS(String pelicula) {
        try {
            // Simula latencia de descarga desde almacenamiento remoto tipo AWS/S3
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("AWS: Descarga de " + pelicula + " completada.");

        // Simula datos binarios de video
        return new byte[50000];
    }
}