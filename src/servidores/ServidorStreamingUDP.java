package servidores;

import compartido.FragmentoVideo;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Servidor de Streaming UDP (Puerto 6000) - Data Plane.
 *
 * Responsabilidades:
 * - Recibir solicitudes de reproducción por UDP
 * - Obtener datos de video desde la Cache Web (o Backend AWS)
 * - Transmitir fragmentos de video como objetos serializados (marshalling)
 * - Cada fragmento incluye número de secuencia para detección de pérdida
 *
 * Comunicación:
 * - UDP con Clientes (Data Plane, directo, no pasa por Gateway)
 * - Interno con CacheWeb -> BackendAWS (almacenamiento de video)
 */
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

                    // Un hilo por cada transmisión (concurrencia)
                    new Thread(() -> despacharVideo(
                        socket, ipCliente, puertoCliente, pelicula, fragmentos
                    )).start();
                }
            }

        } catch (Exception e) {
            System.err.println("[STREAMING] Error crítico: " + e.getMessage());
        }
    }

    /**
     * Transmite los fragmentos de video como objetos FragmentoVideo serializados.
     *
     * Cada fragmento se serializa (marshalling) a bytes y se envía como
     * DatagramPacket UDP. Incluye número de secuencia para que el cliente
     * pueda detectar si se perdió algún paquete.
     */
    private static void despacharVideo(
        DatagramSocket socket,
        InetAddress ip,
        int puerto,
        String pelicula,
        int fragmentos
    ) {
        try {
            // Obtener datos del video (desde cache o backend AWS)
            byte[] datosCompletos = cache.obtenerVideo(pelicula, fragmentos);
            int tamanioPorFragmento = datosCompletos.length / fragmentos;

            System.out.println("[STREAMING] Iniciando transmisión: " + pelicula +
                               " -> " + ip.getHostAddress() + ":" + puerto);

            for (int i = 1; i <= fragmentos; i++) {
                // Extraer la porción de datos para este fragmento
                int inicio = (i - 1) * tamanioPorFragmento;
                int fin = Math.min(i * tamanioPorFragmento, datosCompletos.length);
                byte[] datosFragmento = new byte[fin - inicio];
                System.arraycopy(datosCompletos, inicio, datosFragmento, 0, datosFragmento.length);

                // Crear objeto FragmentoVideo con número de secuencia
                FragmentoVideo fragmento = new FragmentoVideo(i, fragmentos, pelicula, datosFragmento);

                // Serializar el objeto (marshalling) a bytes para UDP
                byte[] bytesSerializados = serializarObjeto(fragmento);

                DatagramPacket paquete = new DatagramPacket(
                    bytesSerializados, bytesSerializados.length, ip, puerto
                );

                socket.send(paquete);

                System.out.println("[STREAMING] Enviado: " + fragmento);

                // Simular tasa de transmisión (1 fragmento por segundo)
                Thread.sleep(1000);
            }

            // Enviar señal de fin de streaming
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

    /**
     * Serializa un objeto a un array de bytes (marshalling manual para UDP).
     * Esto es necesario porque UDP trabaja con DatagramPacket (bytes crudos),
     * no con ObjectOutputStream como TCP.
     */
    private static byte[] serializarObjeto(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        return baos.toByteArray();
    }
}