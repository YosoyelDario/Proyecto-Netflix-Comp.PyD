package servidores;

import compartido.FragmentoVideo;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class ServidorStreamingUDP {
    private static int puertoUDP = 6000;
    private static String rutaVideos = "data/videos";
    private static CacheWeb cache;

    public static void main(String[] args) {
        if (args.length >= 1) puertoUDP = Integer.parseInt(args[0]);
        if (args.length >= 2) rutaVideos = args[1];

        cache = new CacheWeb(rutaVideos);

        try (DatagramSocket socket = new DatagramSocket(puertoUDP)) {
            System.out.println("==================================================");
            System.out.println("[STREAMING] Servidor Streaming UDP iniciado");
            System.out.println("[STREAMING] Escuchando en puerto: " + puertoUDP);
            System.out.println("[STREAMING] Almacenamiento de video: " + rutaVideos);
            System.out.println("==================================================");

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket peticion = new DatagramPacket(buffer, buffer.length);

                socket.receive(peticion);

                String datosStr = new String(
                    peticion.getData(), 0, peticion.getLength()
                ).trim();

                if (datosStr.startsWith("PLAY:")) {
                    String[] partes = datosStr.split(":");

                    if (partes.length < 3) {
                        System.err.println("[STREAMING] Petición inválida: " + datosStr);
                        continue;
                    }

                    String pelicula = partes[1];
                    int fragmentos = Integer.parseInt(partes[2]);

                    InetAddress ipCliente = peticion.getAddress();
                    int puertoCliente = peticion.getPort();

                    System.out.println("\n[STREAMING] Solicitud de: " + ipCliente.getHostAddress() +
                                       ":" + puertoCliente + " -> " + pelicula + " (" + fragmentos + " fragmentos)");
                    new Thread(() -> despacharVideo(
                        socket, ipCliente, puertoCliente, pelicula, fragmentos
                    )).start();
                }
            }

        } catch (Exception e) {
            System.err.println("[STREAMING] Error crítico: " + e.getMessage());
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
            
            byte[] datosCompletos = cache.obtenerVideo(pelicula, fragmentos);
            int tamanioPorFragmento = datosCompletos.length / fragmentos;

            System.out.println("[STREAMING] Iniciando transmisión: " + pelicula + " -> " + ip.getHostAddress() + ":" + puerto);

            for (int i = 1; i <= fragmentos; i++) {
                
                int inicio = (i - 1) * tamanioPorFragmento;
                int fin = Math.min(i * tamanioPorFragmento, datosCompletos.length);
                byte[] datosFragmento = new byte[fin - inicio];
                System.arraycopy(datosCompletos, inicio, datosFragmento, 0, datosFragmento.length);

                
                FragmentoVideo fragmento = new FragmentoVideo(i, fragmentos, pelicula, datosFragmento);

                
                byte[] bytesSerializados = serializarObjeto(fragmento);

                DatagramPacket paquete = new DatagramPacket(
                    bytesSerializados, bytesSerializados.length, ip, puerto
                );

                socket.send(paquete);

                System.out.println("[STREAMING] Enviado: " + fragmento);

                
                Thread.sleep(1000);
            }

            
            FragmentoVideo fin = new FragmentoVideo(pelicula);
            byte[] bytesFin = serializarObjeto(fin);

            DatagramPacket paqueteFin = new DatagramPacket(
                bytesFin, bytesFin.length, ip, puerto
            );

            socket.send(paqueteFin);

            System.out.println("[STREAMING] Transmisión completada: " + pelicula);

        } catch (Exception e) {
            System.err.println("[STREAMING] Fallo en transmisión a " + ip.getHostAddress() + ": " + e.getMessage());
        }
    }
    private static byte[] serializarObjeto(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }
}