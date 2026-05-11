package cliente;

import compartido.FragmentoVideo;
import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente Interactivo - App Netflix.
 *
 * Capa de Presentación: Interfaz de navegación (menú) + Reproductor multimedia.
 *
 * Transparencia de ubicación: el cliente SOLO conoce la IP del Gateway ZUUL.
 * No necesita saber cuántos servidores hay detrás ni dónde están.
 *
 * Control Plane (TCP via Gateway): login, catálogo, búsqueda, perfil, elegir película
 * Data Plane (UDP directo): streaming de video
 * Subtítulos (TCP directo): paralelo al streaming
 */
public class ClienteInteractivo {
    // Gateway ZUUL (punto de entrada único)
    private static String hostGateway = "localhost";
    private static int puertoGateway = 4000;

    // Data Plane (conexión directa, no pasa por Gateway)
    private static String hostUDP = "localhost";
    private static int puertoUDP = 6000;

    // Subtítulos (conexión directa, paralela al streaming)
    private static String hostSubtitulos = "localhost";
    private static int puertoSubtitulos = 7000;

    private static boolean sesionIniciada = false;
    private static String tokenSesion = null;

    public static void main(String[] args) {
        cargarConfiguracion(args);

        System.out.println("============================================");
        System.out.println("       NETFLIX - Cliente Interactivo");
        System.out.println("============================================");
        System.out.println("Gateway ZUUL:       " + hostGateway + ":" + puertoGateway);
        System.out.println("Servidor Streaming: " + hostUDP + ":" + puertoUDP);
        System.out.println("Servidor Subtítulos:" + hostSubtitulos + ":" + puertoSubtitulos);
        System.out.println("============================================\n");

        Scanner scanner = new Scanner(System.in);
        boolean ejecutando = true;

        while (ejecutando) {
            System.out.println("\n--- NETFLIX INTERACTIVO ---");
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Ver catálogo");
            System.out.println("3. Buscar título");
            System.out.println("4. Ver perfil");
            System.out.println("5. Reproducir película");
            System.out.println("6. Salir");
            System.out.print("Selección: ");

            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1" -> menuLogin(scanner);
                case "2" -> menuVerCatalogo();
                case "3" -> menuBuscar(scanner);
                case "4" -> menuVerPerfil();
                case "5" -> menuReproducir(scanner);
                case "6" -> {
                    enviarAlGateway(new Peticion("SALIR", ""));
                    System.out.println("Cerrando cliente...");
                    ejecutando = false;
                }
                default -> System.out.println("Opción no válida.");
            }
        }

        scanner.close();
    }

    // =========================================================================
    // MENÚS DE INTERACCIÓN
    // =========================================================================

    private static void menuLogin(Scanner scanner) {
        System.out.print("Usuario: ");
        String usuario = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();

        Respuesta resp = enviarAlGateway(new Peticion("LOGIN", usuario + ":" + password));
        if (resp == null) return;

        if (resp.codigo.equals("LOGIN_OK")) {
    String payload = (String) resp.payload;
    String[] partes = payload.split("\\|");
    tokenSesion = partes[0];           // partes[0] = token
    String username = partes.length > 1 ? partes[1] : "";
    String plan = partes.length > 2 ? partes[2] : "";
    sesionIniciada = true;
    System.out.println("\n  Bienvenido, " + username + " [" + plan + "]");
}else {
            mostrarError(resp);
        }
    }

    private static void menuVerCatalogo() {
        if (!verificarSesion()) return;

        Respuesta resp = enviarAlGateway(new Peticion("VER_CATALOGO", tokenSesion));
        if (resp == null) return;

        procesarRespuesta(resp);
    }

    private static void menuBuscar(Scanner scanner) {
        if (!verificarSesion()) return;

        System.out.print("Término de búsqueda: ");
        String query = scanner.nextLine().trim();

        // Formato: "token|query"
        Respuesta resp = enviarAlGateway(new Peticion("BUSCAR", tokenSesion + "|" + query));
        if (resp == null) return;

        procesarRespuesta(resp);
    }

    private static void menuVerPerfil() {
        if (!verificarSesion()) return;

        Respuesta resp = enviarAlGateway(new Peticion("VER_PERFIL", tokenSesion));
        if (resp == null) return;

        procesarRespuesta(resp);
    }

    private static void menuReproducir(Scanner scanner) {
        if (!verificarSesion()) return;

        System.out.print("Título de la película: ");
        String titulo = scanner.nextLine().trim();

        // Formato: "token|titulo"
        Respuesta resp = enviarAlGateway(new Peticion("ELEGIR_PELICULA", tokenSesion + "|" + titulo));
        if (resp == null) return;

        if (resp.codigo.equals("STREAM_INFO")) {
            Pelicula pelicula = (Pelicula) resp.payload;
            System.out.println("\n  Streaming autorizado: " + pelicula.titulo);

            System.out.print("  Idioma de subtítulos (es/en): ");
            String idioma = scanner.nextLine().trim();

            if (idioma.isEmpty()) {
                idioma = "es";
            }

            iniciarReproduccionDistribuida(pelicula, idioma);
        } else {
            mostrarError(resp);
        }
    }

    // =========================================================================
    // COMUNICACIÓN CON GATEWAY (Control Plane TCP)
    // =========================================================================

    /**
     * Envía una petición al Gateway ZUUL y recibe la respuesta.
     * Cada petición abre una conexión TCP nueva (modelo stateless).
     */
    private static Respuesta enviarAlGateway(Peticion peticion) {
        try (
            Socket socket = new Socket(hostGateway, puertoGateway);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.writeObject(peticion);
            out.flush();

            Object resp = in.readObject();

            if (resp instanceof Respuesta) {
                return (Respuesta) resp;
            }

            return null;

        } catch (java.net.ConnectException e) {
            System.err.println("\n  Error de conexión: No se pudo conectar al Gateway.");
            System.err.println("  Verifique que el Gateway esté activo en " + hostGateway + ":" + puertoGateway);
            return null;

        } catch (Exception e) {
            System.err.println("\n  Error de comunicación: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // FUNCIÓN B: REPRODUCCIÓN DISTRIBUIDA
    // =========================================================================

    /**
     * Inicia la reproducción distribuida: video UDP + subtítulos TCP en paralelo.
     * Dos hilos independientes que corren al mismo tiempo.
     * Si los subtítulos fallan, el video continúa (fallo independiente).
     */
    private static void iniciarReproduccionDistribuida(Pelicula pelicula, String idioma) {
        Thread hiloVideo = new Thread(() -> iniciarStreamingUDP(pelicula));
        Thread hiloSubtitulos = new Thread(() -> iniciarSubtitulosTCP(pelicula, idioma));

        hiloVideo.start();
        hiloSubtitulos.start();

        try {
            hiloVideo.join();
            hiloSubtitulos.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("  La reproducción fue interrumpida.");
        }
    }

    /**
     * Recibe fragmentos de video por UDP.
     * Cada paquete es un objeto FragmentoVideo serializado (marshalling).
     * Se deserializa para obtener número de secuencia, datos y estado.
     */
    private static void iniciarStreamingUDP(Pelicula pelicula) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            // Timeout de 15 segundos para detectar crash del servidor
            udpSocket.setSoTimeout(15000);

            InetAddress ip = InetAddress.getByName(hostUDP);

            // Enviar solicitud de reproducción
            String mensaje = "PLAY:" + pelicula.titulo + ":" + pelicula.fragmentos;
            byte[] bufferSalida = mensaje.getBytes();

            DatagramPacket paqueteSalida = new DatagramPacket(
                bufferSalida, bufferSalida.length, ip, puertoUDP
            );

            udpSocket.send(paqueteSalida);

            System.out.println("\n  --- REPRODUCIENDO: " + pelicula.titulo + " ---");
            System.out.println("  Conectado a streaming UDP: " + hostUDP + ":" + puertoUDP);

            // Buffer grande para recibir objetos serializados
            byte[] bufferEntrada = new byte[65535];
            int ultimaSecuencia = 0;

            while (true) {
                DatagramPacket paqueteEntrada = new DatagramPacket(
                    bufferEntrada, bufferEntrada.length
                );

                try {
                    udpSocket.receive(paqueteEntrada);
                } catch (java.net.SocketTimeoutException e) {
                    System.err.println("  Timeout: Se perdió la conexión con el servidor de streaming.");
                    System.err.println("  Error de reproducción. Reintentando conexión...");
                    break;
                }

                // Deserializar el objeto FragmentoVideo (unmarshalling)
                byte[] datosRecibidos = new byte[paqueteEntrada.getLength()];
                System.arraycopy(paqueteEntrada.getData(), 0, datosRecibidos, 0, paqueteEntrada.getLength());

                FragmentoVideo fragmento = deserializarFragmento(datosRecibidos);

                if (fragmento == null) {
                    System.err.println("  Error al deserializar fragmento.");
                    continue;
                }

                // Señal de fin de streaming
                if (fragmento.esFin) {
                    break;
                }

                // Detección de gap en secuencia (paquete perdido)
                if (fragmento.numeroSecuencia != ultimaSecuencia + 1) {
                    int perdidos = fragmento.numeroSecuencia - ultimaSecuencia - 1;
                    System.err.println("  [AVISO] Gap detectado: " + perdidos + " fragmento(s) perdido(s) entre #" +
                                       ultimaSecuencia + " y #" + fragmento.numeroSecuencia);
                }

                ultimaSecuencia = fragmento.numeroSecuencia;

                // Mostrar info del fragmento recibido
                System.out.println("  " + fragmento);
            }

            System.out.println("  --- VIDEO FINALIZADO ---");

        } catch (Exception e) {
            System.err.println("  Error en flujo UDP: " + e.getMessage());
        }
    }

    /**
     * Deserializa bytes UDP a un objeto FragmentoVideo (unmarshalling).
     */
    private static FragmentoVideo deserializarFragmento(byte[] datos) {
        try (
            ByteArrayInputStream bais = new ByteArrayInputStream(datos);
            ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            return (FragmentoVideo) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recibe subtítulos por TCP, paralelo al streaming.
     * Envía la solicitud con nombre de película e idioma.
     */
    private static void iniciarSubtitulosTCP(Pelicula pelicula, String idioma) {
        try (
            Socket socketSub = new Socket(hostSubtitulos, puertoSubtitulos);
            java.io.PrintWriter outSub = new java.io.PrintWriter(socketSub.getOutputStream(), true);
            java.io.BufferedReader inSub = new java.io.BufferedReader(
                new java.io.InputStreamReader(socketSub.getInputStream())
            )
        ) {
            // Enviar solicitud: "pelicula:idioma"
            outSub.println(pelicula.titulo + ":" + idioma);

            System.out.println("  --- SUBTÍTULOS (" + idioma.toUpperCase() + "): " + pelicula.titulo + " ---");

            String linea;

            while ((linea = inSub.readLine()) != null) {
                if (linea.equals("FIN_SUBTITULOS")) {
                    break;
                }

                if (linea.startsWith("ERROR:")) {
                    System.err.println("  SUB ERROR: " + linea.substring(6));
                    break;
                }

                if (linea.startsWith("IDIOMA_NO_DISPONIBLE:")) {
                    System.err.println("  " + linea.substring(21));
                    break;
                }

                System.out.println("  SUB: " + linea);
            }

            System.out.println("  --- SUBTÍTULOS FINALIZADOS ---");

        } catch (java.net.ConnectException e) {
            // Fallo independiente: el servidor de subtítulos cayó
            System.err.println("  Subtítulos no disponibles. La reproducción continúa sin subtítulos.");
        } catch (Exception e) {
            System.err.println("  Error en subtítulos: " + e.getMessage());
            System.err.println("  La reproducción continúa sin subtítulos.");
        }
    }

    // =========================================================================
    // UTILIDADES
    // =========================================================================

    private static boolean verificarSesion() {
        if (!sesionIniciada || tokenSesion == null) {
            System.out.println("\n  Debe iniciar sesión primero (opción 1).");
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static void procesarRespuesta(Respuesta resp) {
        switch (resp.codigo) {
            case "OK" -> {
                Object payload = resp.payload;

                if (payload instanceof List) {
                    List<Pelicula> catalogo = (List<Pelicula>) payload;
                    System.out.println("\n  === CATÁLOGO (" + catalogo.size() + " títulos) ===");

                    for (Pelicula p : catalogo) {
                        System.out.println("  " + p.toStringCorto());
                    }
                } else {
                    System.out.println("\n  " + payload);
                }
            }

            case "RESULTADOS" -> {
                List<Pelicula> resultados = (List<Pelicula>) resp.payload;
                System.out.println("\n  === RESULTADOS DE BÚSQUEDA (" + resultados.size() + ") ===");

                for (Pelicula p : resultados) {
                    System.out.println("  " + p.toString());
                }
            }

            default -> mostrarError(resp);
        }
    }

    private static void mostrarError(Respuesta resp) {
        switch (resp.codigo) {
            case "401" -> {
                System.err.println("\n  [ERROR 401] " + resp.payload);
                sesionIniciada = false;
                tokenSesion = null;
            }
            case "403" -> System.err.println("\n  [ERROR 403] " + resp.payload);
            case "404" -> System.err.println("\n  [ERROR 404] " + resp.payload);
            case "500" -> {
                System.err.println("\n  [ERROR 500] " + resp.payload);
                System.err.println("  Error interno. Reintente más tarde.");
            }
            case "503" -> {
                System.err.println("\n  [ERROR 503] " + resp.payload);
                System.err.println("  Servicio temporalmente no disponible. Reintente.");
            }
            default -> System.err.println("\n  [" + resp.codigo + "] " + resp.payload);
        }
    }

    private static void cargarConfiguracion(String[] args) {
        /*
         * args[0] = host Gateway ZUUL
         * args[1] = puerto Gateway ZUUL
         * args[2] = host Servidor Streaming UDP
         * args[3] = puerto Servidor Streaming UDP
         * args[4] = host Servidor Subtítulos TCP
         * args[5] = puerto Servidor Subtítulos TCP
         *
         * Ejemplo (todo local):
         * java -cp build/classes cliente.ClienteInteractivo localhost 4000 localhost 6000 localhost 7000
         *
         * Ejemplo (distribuido en red):
         * java -cp build/classes cliente.ClienteInteractivo 192.168.1.10 4000 192.168.1.30 6000 192.168.1.30 7000
         */

        if (args.length >= 1) hostGateway = args[0];
        if (args.length >= 2) puertoGateway = Integer.parseInt(args[1]);
        if (args.length >= 3) hostUDP = args[2];
        if (args.length >= 4) puertoUDP = Integer.parseInt(args[3]);
        if (args.length >= 5) hostSubtitulos = args[4];
        if (args.length >= 6) puertoSubtitulos = Integer.parseInt(args[5]);
    }
}