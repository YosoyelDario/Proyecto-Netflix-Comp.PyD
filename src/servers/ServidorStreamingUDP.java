import java.net.*;
import java.util.concurrent.*;

public class ServidorStreamingUDP {
    private static final int PUERTO_UDP = 6000;
    private static final CacheWeb cache = new CacheWeb();

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PUERTO_UDP);
        System.out.println("Servidor Streaming iniciado. Puerto: " + PUERTO_UDP);

        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);
            socket.receive(peticion);

            // Payload esperado: "titulo_pelicula|cantidad_fragmentos"
            String datosStr = new String(peticion.getData(), 0, peticion.getLength()).trim();
            if (datosStr.startsWith("PLAY:")) {
                String[] partes = datosStr.split(":");
                String pelicula = partes[1];
                int fragmentos = Integer.parseInt(partes[2]);

                new Thread(() -> despacharVideo(socket, peticion.getAddress(), peticion.getPort(), pelicula, fragmentos)).start();
            }
        }
    }

    private static void despacharVideo(DatagramSocket socket, InetAddress ip, int puerto, String pelicula, int fragmentos) {
        try {
            // 1. Verificar Caché y Backend
            byte[] datosVideo = cache.obtenerVideo(pelicula);

            // 2. Transmisión UDP
            for (int i = 1; i <= fragmentos; i++) {
                String frag = "[FRAG " + i + "/" + fragmentos + "] " + pelicula.toUpperCase() + " DATA SIZE: " + datosVideo.length + " bytes";
                DatagramPacket paquete = new DatagramPacket(frag.getBytes(), frag.length(), ip, puerto);
                socket.send(paquete);
                Thread.sleep(1000); // Emulación de bitrate
            }
            String fin = "FIN_STREAMING";
            socket.send(new DatagramPacket(fin.getBytes(), fin.length(), ip, puerto));
        } catch (Exception e) {
            System.err.println("Fallo en transmisión UDP a " + ip.getHostAddress());
        }
    }
}

// Nodo: Cache Servidor Web
class CacheWeb {
    private final ConcurrentHashMap<String, byte[]> almacenamientoMemoria = new ConcurrentHashMap<>();
    private final BackendAWS backend = new BackendAWS();

    public byte[] obtenerVideo(String pelicula) {
        if (almacenamientoMemoria.containsKey(pelicula)) {
            System.out.println("CACHE HIT: " + pelicula + " servido desde memoria.");
            return almacenamientoMemoria.get(pelicula);
        } else {
            System.out.println("CACHE MISS: " + pelicula + ". Solicitando a Backend AWS...");
            byte[] datos = backend.descargarVideoHTTPS(pelicula);
            almacenamientoMemoria.put(pelicula, datos);
            return datos;
        }
    }
}

// Nodo: Backend AWS (Almacenamiento de Video)
class BackendAWS {
    public byte[] descargarVideoHTTPS(String pelicula) {
        try {
            // Simula latencia de descarga desde S3/AWS vía HTTPS
            Thread.sleep(3000); 
        } catch (InterruptedException ignored) {}
        
        System.out.println("AWS: Descarga de " + pelicula + " completada.");
        // Simula el binario del video
        return new byte[50000]; 
    }
}