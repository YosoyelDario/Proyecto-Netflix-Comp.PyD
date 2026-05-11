package client;
import compartido.Pelicula;
import compartido.Peticion;
import compartido.PeticionLogin;
import compartido.Respuesta;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;

/** 
 * Cliente interactivo de cmd para la interacción con el servidor de Netflix
 */

public class ClienteInteractivo {

    // Variables de red base
    private static final String HOST = "localhost";
    private static final int PORT = 5000;      // Servidor Catálogo (Futuro Gateway)
    private static final int AUTH_PORT = 5001; // Servidor Autenticación

    // Variables de configuración de los otros servicios (Agregadas para evitar errores)
    private static String hostTCP = HOST;
    private static int puertoTCP = PORT;
    private static String hostUDP = HOST;
    private static int puertoUDP = 6000;
    private static String hostSubtitulos = HOST;
    private static int puertoSubtitulos = 7000;
    
    // Estado del cliente
    private static boolean sesionIniciada = false;

    public static void main(String[] args) {

        cargarConfiguracion(args);
        // Se utilizan Object Streams para el Marshalling
        try (Scanner scanner = new Scanner(System.in)) {
            
            System.out.println("--- INICIO DE SESIÓN ---");
            System.out.print("Usuario: ");
            String user = scanner.nextLine();
            System.out.print("Contraseña: ");
            String pass = scanner.nextLine();
            
            // 1. Conexión directa temporal al Servidor de Autenticación
            try (Socket authSocket = new Socket(HOST, AUTH_PORT);
                 ObjectOutputStream outAuth = new ObjectOutputStream(authSocket.getOutputStream());
                 ObjectInputStream inAuth = new ObjectInputStream(authSocket.getInputStream())) {
                 
                outAuth.writeObject(new PeticionLogin(user, pass));
                Respuesta authResp = (Respuesta) inAuth.readObject();

                if (!"OK".equals(authResp.codigo)) {
                    System.err.println("Autenticación fallida: " + authResp.payload);
                    return;
                }

                sesionIniciada = true;
                System.out.println("Autenticación exitosa! Token: " + authResp.payload);
            }

            // 2. Conexión al servidor de catálogo
            try (Socket socketTCP = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream())) {

                System.out.println("Conectado al Servidor de Control (Marshalling activo).");

                while (true) {
                    System.out.println("\n--- NETFLIX INTERACTIVO ---");
                    System.out.println("1. Ver Catálogo");
                    System.out.println("2. Ver Perfil / Mi Lista");
                    System.out.println("3. Reproducir Película");
                    System.out.println("4. Salir");
                    System.out.print("Selección: ");
                    
                    String opcion = scanner.nextLine();
                    Peticion p = null;

                    if (opcion.equals("1")) {
                        p = new Peticion("VER_CATALOGO", "");
                    } else if (opcion.equals("2")) {
                    // Submenú para probar las dos funciones nuevas de tu ServidorAutenticacion
                            System.out.print("¿Qué deseas ver? (A = Perfil Completo, B = Solo Mi Lista): ");
                            String subOpt = scanner.nextLine().toUpperCase();
                            
                            if (subOpt.equals("A")) {
                                p = new Peticion("VER_PERFIL", user);
                            } else {
                                p = new Peticion("VER_MI_LISTA", user);
                            }
                    } else if (opcion.equals("3")) {
                        System.out.print("Título de la película: ");
                        p = new Peticion("ELEGIR_PELICULA", scanner.nextLine());
                    } else if (opcion.equals("4")) {
                        out.writeObject(new Peticion("SALIR", ""));
                        break;
                    }

                    if (p != null) {
                        if (opcion.equals("2")) {
                            // Temporalmente dirigimos la petición de perfil directo al Auth Server
                            try (Socket authSocket = new Socket(HOST, AUTH_PORT);
                                ObjectOutputStream outAuth = new ObjectOutputStream(authSocket.getOutputStream());
                                ObjectInputStream inAuth = new ObjectInputStream(authSocket.getInputStream())) {
                                outAuth.writeObject(p);
                                procesarRespuesta((Respuesta) inAuth.readObject());
                            }
                            continue;
                        }

                        // Las demás peticiones van al servidor de catálogo
                        out.writeObject(p); // Envío del objeto serializado
                        out.flush();
                        
                        // Recepción de la respuesta serializada
                        Respuesta resp = (Respuesta) in.readObject();
                        procesarRespuesta(resp);
                    }
                }
            }
        } catch (ConnectException e) {
            System.err.println("Error de conexión: Asegúrate de que los servidores estén corriendo primero.");
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

        if (args.length >= 1) hostTCP = args[0];
        if (args.length >= 2) puertoTCP = Integer.parseInt(args[1]);
        if (args.length >= 3) hostUDP = args[2];
        if (args.length >= 4) puertoUDP = Integer.parseInt(args[3]);
        if (args.length >= 5) hostSubtitulos = args[4];
        if (args.length >= 6) puertoSubtitulos = Integer.parseInt(args[5]);
    }

   private static void procesarRespuesta(Respuesta respuesta) {
        switch (respuesta.codigo) {
            case "LOGIN_OK" -> {
                sesionIniciada = true;
                System.out.println("-> " + respuesta.payload);
            }

            case "OK" -> {
                if (respuesta.payload instanceof List) {
                    System.out.println("-> Tu Lista de Películas: ");
                    List<?> lista = (List<?>) respuesta.payload;
                    for (Object item : lista) {
                        System.out.println("   - " + item);
                    }
                } else {
                    System.out.println("-> Servidor: " + respuesta.payload);
                }
            }

            case "STREAM_INFO" -> {
                Pelicula pelicula = (Pelicula) respuesta.payload;
                iniciarReproduccionDistribuida(pelicula);
            }

            case "ERROR" -> System.err.println("-> Error del Servidor: " + respuesta.payload);
            default -> System.err.println("-> Respuesta desconocida: " + respuesta.codigo);
        }
    }

    /** Método para iniciar la reproducción distribuida de una película */

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

    /** Método para iniciar el streaming UDP de una película */
    private static void iniciarStreamingUDP(Pelicula pelicula) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress ip = InetAddress.getByName(hostUDP);
            String mensaje = "PLAY:" + pelicula.titulo + ":" + pelicula.fragmentos;
            byte[] bufferSalida = mensaje.getBytes();

            DatagramPacket paqueteSalida = new DatagramPacket(bufferSalida, bufferSalida.length, ip, puertoUDP);
            udpSocket.send(paqueteSalida);

            System.out.println("--- REPRODUCIENDO: " + pelicula.titulo + " ---");
            System.out.println("Conectado al servidor UDP: " + hostUDP + ":" + puertoUDP);

            byte[] bufferEntrada = new byte[1024];

            while (true) {
                DatagramPacket paqueteEntrada = new DatagramPacket(bufferEntrada, bufferEntrada.length);
                udpSocket.receive(paqueteEntrada);
                String data = new String(paqueteEntrada.getData(), 0, paqueteEntrada.getLength());

                if (data.equals("FIN_STREAMING")) break;

                System.out.println(data);
            }

            System.out.println("--- VIDEO FINALIZADO ---");

        } catch (Exception e) {
            System.err.println("Error en flujo UDP: " + e.getMessage());
        }
    }
   /** Método para iniciar los subtítulos TCP de una película*/
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
                if (linea.equals("FIN_SUBTITULOS"))  break;
                System.out.println("SUB: " + linea);
            }

            System.out.println("--- SUBTÍTULOS FINALIZADOS ---");

        } catch (Exception e) {
            System.err.println("Error en subtítulos: " + e.getMessage());
            System.err.println("La reproducción de video puede continuar sin subtítulos.");
        }
    }

 
}