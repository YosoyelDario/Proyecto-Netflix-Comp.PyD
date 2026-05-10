package cliente;

import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClienteInteractivo {
    private static final String HOST_TCP = "localhost";
    private static final int PORT_TCP = 5000;

    private static final String HOST_UDP = "localhost";
    private static final int PORT_UDP = 6000;

    public static void main(String[] args) {
        try (
            Socket socketTCP = new Socket(HOST_TCP, PORT_TCP);
            ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream());
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Conectado al Servidor de Control TCP.");
            System.out.println("Marshalling activo mediante Object Streams.");

            while (true) {
                System.out.println("\n--- NETFLIX INTERACTIVO ---");
                System.out.println("1. Ver Catálogo");
                System.out.println("2. Ver Perfil");
                System.out.println("3. Reproducir Película");
                System.out.println("4. Salir");
                System.out.print("Selección: ");

                String opcion = scanner.nextLine();
                Peticion peticion = null;

                if (opcion.equals("1")) {
                    peticion = new Peticion("VER_CATALOGO", "");

                } else if (opcion.equals("2")) {
                    System.out.print("Nombre de usuario: ");
                    String usuario = scanner.nextLine();

                    peticion = new Peticion("VER_PERFIL", usuario);

                } else if (opcion.equals("3")) {
                    System.out.print("Título de la película: ");
                    String titulo = scanner.nextLine();

                    peticion = new Peticion("ELEGIR_PELICULA", titulo);

                } else if (opcion.equals("4")) {
                    out.writeObject(new Peticion("SALIR", ""));
                    out.flush();
                    System.out.println("Cerrando cliente...");
                    break;

                } else {
                    System.out.println("Opción no válida.");
                    continue;
                }

                out.writeObject(peticion);
                out.flush();

                Respuesta respuesta = (Respuesta) in.readObject();
                procesarRespuesta(respuesta);
            }

        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private static void procesarRespuesta(Respuesta respuesta) {
        switch (respuesta.codigo) {
            case "OK":
                System.out.println("-> Servidor: " + respuesta.payload);
                break;

            case "STREAM_INFO":
                Pelicula pelicula = (Pelicula) respuesta.payload;
                iniciarStreamingUDP(pelicula);
                break;

            case "ERROR":
                System.err.println("-> Error del Servidor: " + respuesta.payload);
                break;

            default:
                System.err.println("-> Respuesta desconocida: " + respuesta.codigo);
                break;
        }
    }

    private static void iniciarStreamingUDP(Pelicula pelicula) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress ip = InetAddress.getByName(HOST_UDP);

            String mensaje = "PLAY:" + pelicula.titulo + ":" + pelicula.fragmentos;
            byte[] bufferSalida = mensaje.getBytes();

            DatagramPacket paqueteSalida = new DatagramPacket(
                bufferSalida,
                bufferSalida.length,
                ip,
                PORT_UDP
            );

            udpSocket.send(paqueteSalida);

            System.out.println("--- REPRODUCIENDO: " + pelicula.titulo + " ---");

            byte[] bufferEntrada = new byte[1024];

            while (true) {
                DatagramPacket paqueteEntrada = new DatagramPacket(
                    bufferEntrada,
                    bufferEntrada.length
                );

                udpSocket.receive(paqueteEntrada);

                String data = new String(
                    paqueteEntrada.getData(),
                    0,
                    paqueteEntrada.getLength()
                );

                if (data.equals("FIN_STREAMING")) {
                    break;
                }

                System.out.println(data);
            }

            System.out.println("--- VIDEO FINALIZADO ---");

        } catch (Exception e) {
            System.err.println("Error en flujo UDP: " + e.getMessage());
        }
    }
}