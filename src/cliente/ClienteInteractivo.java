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
    private static String hostTCP = "localhost";
    private static int puertoTCP = 5000;

    private static String hostUDP = "localhost";
    private static int puertoUDP = 6000;
    
    private static String hostSubtitulos = "localhost";
    private static int puertoSubtitulos = 7000;
    
    private static boolean sesionIniciada = false;

    public static void main(String[] args) {
        cargarConfiguracion(args);

        System.out.println("Configuración del cliente:");
        System.out.println("Servidor TCP/Catálogo: " + hostTCP + ":" + puertoTCP);
        System.out.println("Servidor UDP/Streaming: " + hostUDP + ":" + puertoUDP);
        System.out.println("Servidor TCP/Subtítulos: " + hostSubtitulos + ":" + puertoSubtitulos);

        try (
            Socket socketTCP = new Socket(hostTCP, puertoTCP);
            ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream());
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("\nConectado al Servidor de Control TCP.");
            System.out.println("Marshalling activo mediante Object Streams.");

            OUTER:
            OUTER_1:
            while (true) {
                System.out.println("\n--- NETFLIX INTERACTIVO ---");
                System.out.println("1. Iniciar sesión");
                System.out.println("2. Ver Catálogo");
                System.out.println("3. Ver Perfil");
                System.out.println("4. Reproducir Película");
                System.out.println("5. Salir");
                System.out.print("Selección: ");
                
                String opcion = scanner.nextLine();
                Peticion peticion = null;
                
                switch (opcion) {
                    case "1" -> {
                        System.out.print("Usuario: ");
                        String usuario = scanner.nextLine();
                        System.out.print("Contraseña: ");
                        String password = scanner.nextLine();
                        peticion = new Peticion("LOGIN", usuario + ":" + password);
                    }
                    case "2" -> peticion = new Peticion("VER_CATALOGO", "");
                    case "3" -> peticion = new Peticion("VER_PERFIL", "");
                    case "4" -> {
                        System.out.print("Título de la película: ");
                        String titulo = scanner.nextLine();
                        peticion = new Peticion("ELEGIR_PELICULA", titulo);
                    }
                    case "5" -> {
                        out.writeObject(new Peticion("SALIR", ""));
                        out.flush();
                        System.out.println("Cerrando cliente...");
                        break OUTER_1;
                    }
                    default -> {
                        System.out.println("Opción no válida.");
                        continue;
                    }
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

    private static void cargarConfiguracion(String[] args) {
        /*
         * Formato esperado:
         *
         * args[0] = IP o host del ServidorCatalogo
         * args[1] = puerto del ServidorCatalogo
         * args[2] = IP o host del ServidorStreamingUDP
         * args[3] = puerto del ServidorStreamingUDP
         * args[4] = IP o host del ServidorSubtitulos
         * args[5] = puerto del ServidorSubtitulos
         *
         * Ejemplo:
         * java -cp build/classes cliente.ClienteInteractivo localhost 5000 localhost 6000 localhost 7000
         */

        if (args.length >= 1) {
            hostTCP = args[0];
        }

        if (args.length >= 2) {
            puertoTCP = Integer.parseInt(args[1]);
        }

        if (args.length >= 3) {
            hostUDP = args[2];
        }

        if (args.length >= 4) {
            puertoUDP = Integer.parseInt(args[3]);
        }

        if (args.length >= 5) {
            hostSubtitulos = args[4];
        }

        if (args.length >= 6) {
            puertoSubtitulos = Integer.parseInt(args[5]);
        }
    }
    
    
    private static void procesarRespuesta(Respuesta respuesta) {
        switch (respuesta.codigo) {
            case "LOGIN_OK" -> {
                sesionIniciada = true;
                System.out.println("-> " + respuesta.payload);
            }

            case "OK" -> System.out.println("-> Servidor: " + respuesta.payload);

            case "STREAM_INFO" -> {
                Pelicula pelicula = (Pelicula) respuesta.payload;
                iniciarReproduccionDistribuida(pelicula);
            }

            case "ERROR" -> System.err.println("-> Error del Servidor: " + respuesta.payload);

            default -> System.err.println("-> Respuesta desconocida: " + respuesta.codigo);
        }
    }

    private static void iniciarStreamingUDP(Pelicula pelicula) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress ip = InetAddress.getByName(hostUDP);

            String mensaje = "PLAY:" + pelicula.titulo + ":" + pelicula.fragmentos;
            byte[] bufferSalida = mensaje.getBytes();

            DatagramPacket paqueteSalida = new DatagramPacket(
                bufferSalida,
                bufferSalida.length,
                ip,
                puertoUDP
            );

            udpSocket.send(paqueteSalida);

            System.out.println("--- REPRODUCIENDO: " + pelicula.titulo + " ---");
            System.out.println("Conectado al servidor UDP: " + hostUDP + ":" + puertoUDP);

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

    private static void iniciarReproduccionDistribuida(Pelicula pelicula) {
        Thread hiloVideo = new Thread(() -> iniciarStreamingUDP(pelicula));
        Thread hiloSubtitulos = new Thread(() -> iniciarSubtitulosTCP(pelicula));

        hiloVideo.start();
        hiloSubtitulos.start();

        try {
            hiloVideo.join();
            hiloSubtitulos.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("La reproducción fue interrumpida.");
        }
    }
    
    private static void iniciarSubtitulosTCP(Pelicula pelicula) {
        try (
            Socket socketSubtitulos = new Socket(hostSubtitulos, puertoSubtitulos);
            java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(socketSubtitulos.getInputStream())
            )
        ) {
            System.out.println("--- SUBTÍTULOS ACTIVOS PARA: " + pelicula.titulo + " ---");
            System.out.println("Conectado al servidor de subtítulos: " + hostSubtitulos + ":" + puertoSubtitulos);

            String linea;

            while ((linea = in.readLine()) != null) {
                if (linea.equals("FIN_SUBTITULOS")) {
                    break;
                }

                System.out.println("SUB: " + linea);
            }

            System.out.println("--- SUBTÍTULOS FINALIZADOS ---");

        } catch (Exception e) {
            System.err.println("Error en subtítulos: " + e.getMessage());
            System.err.println("La reproducción de video puede continuar sin subtítulos.");
        }
    }
}
