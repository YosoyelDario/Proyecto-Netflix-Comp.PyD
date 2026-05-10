package servidores;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServidorStreamingUDP {
    private static int puertoUDP = 6000;
    private static final CacheWeb cache = new CacheWeb();

    public static void main(String[] args) {
        if (args.length >= 1) {
            puertoUDP = Integer.parseInt(args[0]);
        }

        try (DatagramSocket socket = new DatagramSocket(puertoUDP)) {
            System.out.println("Servidor Streaming UDP iniciado. Puerto: " + puertoUDP);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);

                socket.receive(peticion);

                String datosStr = new String(
                    peticion.getData(),
                    0,
                    peticion.getLength()
                ).trim();

                if (datosStr.startsWith("PLAY:")) {
                    String[] partes = datosStr.split(":");

                    if (partes.length < 3) {
                        System.err.println("Petición UDP inválida: " + datosStr);
                        continue;
                    }

                    String pelicula = partes[1];
                    int fragmentos = Integer.parseInt(partes[2]);

                    new Thread(() -> despacharVideo(
                        socket,
                        peticion.getAddress(),
                        peticion.getPort(),
                        pelicula,
                        fragmentos
                    )).start();
                }
            }

        } catch (Exception e) {
            System.err.println("Error en Servidor Streaming UDP: " + e.getMessage());
        }
    }

    private static void despacharVideo(
        DatagramSocket socket,
        InetAddress ip,
        int puerto,
        String pelicula,
        int fragmentos
    ) {
        try {
            byte[] datosVideo = cache.obtenerVideo(pelicula);

            for (int i = 1; i <= fragmentos; i++) {
                String frag = "[FRAG " + i + "/" + fragmentos + "] " +
                        pelicula.toUpperCase() +
                        " DATA SIZE: " +
                        datosVideo.length +
                        " bytes";

                DatagramPacket paquete = new DatagramPacket(
                    frag.getBytes(),
                    frag.getBytes().length,
                    ip,
                    puerto
                );

                socket.send(paquete);

                Thread.sleep(1000);
            }

            String fin = "FIN_STREAMING";

            DatagramPacket paqueteFin = new DatagramPacket(
                fin.getBytes(),
                fin.getBytes().length,
                ip,
                puerto
            );

            socket.send(paqueteFin);

        } catch (Exception e) {
            System.err.println("Fallo en transmisión UDP a " + ip.getHostAddress());
        }
    }
}