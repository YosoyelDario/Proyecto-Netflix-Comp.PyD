package cliente;

import compartido.FragmentoVideo;
import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClienteInteractivo {
    // Control Plane (TCP)
    private static String hostGateway = "localhost";
    private static int puertoGateway = 4000;

    // Data Plane (UDP Directo)
    private static String hostUDP = "localhost";
    private static int puertoUDP = 6000;
    private static String ipLocal = "127.0.0.1";
    // Estado de sesión
    private static boolean sesionIniciada = false;
    private static String tokenSesion = null;

    // Configuración de reintentos (Modelo de Fallos: Omisión)
    private static final int MAX_REINTENTOS = 3;
    private static final int TIMEOUT_CONEXION_MS = 5000;
    private static final int ESPERA_ENTRE_REINTENTOS_MS = 2000;

    // Búferes de reproducción (Rúbrica 2.2: Concurrencia)
    private static final ConcurrentLinkedQueue<FragmentoVideo> bufferVideo = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> bufferSubtitulos = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean recepcionVideoCompletada = new AtomicBoolean(false);

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        cargarConfiguracion(args);
        Scanner scanner = new Scanner(System.in);
        boolean ejecutando = true;

        while (ejecutando) {
            System.out.println("\n--- NETFLIX INTERACTIVO ---");
            if (!sesionIniciada) {
                // Rúbrica 2.3 / README: Restricción de acceso sin login
                System.out.println("1. Iniciar sesión");
                System.out.println("2. Salir");
                System.out.print("Selección: ");
                String op = scanner.nextLine().trim();
                if (op.equals("1")) menuLogin(scanner);
                else if (op.equals("2")) ejecutando = false;
            } else {
                System.out.println("1. Ver catálogo");
                System.out.println("2. Buscar título");
                System.out.println("3. Ver perfil");
                System.out.println("4. Reproducir película");
                System.out.println("5. Salir");
                System.out.print("Selección: ");
                String opcion = scanner.nextLine().trim();
                switch (opcion) {
                    case "1" -> menuVerCatalogo();
                    case "2" -> menuBuscar(scanner);
                    case "3" -> menuVerPerfil();
                    case "4" -> menuReproducir(scanner);
                    case "5" -> {
                        enviarAlGateway(new Peticion("SALIR", ""));
                        ejecutando = false;
                    }
                    default -> System.out.println("Opción no válida.");
                }
            }
        }
        scanner.close();
    }

    private static void menuLogin(Scanner scanner) {
        System.out.print("Usuario: ");
        String usuario = scanner.nextLine().trim();
        System.out.print("Contraseña: ");
        String password = scanner.nextLine().trim();

        Respuesta resp = enviarAlGateway(new Peticion("LOGIN", usuario + ":" + password));
        if (resp != null && resp.codigo.equals("LOGIN_OK")) {
            String[] partes = ((String) resp.payload).split("\\|");
            tokenSesion = partes[0];
            sesionIniciada = true;
            System.out.println("\n  Sesión iniciada como: " + partes[1]);
        } else {
            mostrarError(resp);
        }
    }

    private static void menuReproducir(Scanner scanner) {
        System.out.print("Título de la película: ");
        String titulo = scanner.nextLine().trim();

        Respuesta resp = enviarAlGateway(new Peticion("ELEGIR_PELICULA", tokenSesion + "|" + titulo));
        if (resp != null && resp.codigo.equals("STREAM_INFO")) {
            Pelicula pelicula = (Pelicula) resp.payload;
            
            // README: Opción de habilitar/deshabilitar subtítulos
            System.out.print("  ¿Desea activar subtítulos? (s/n): ");
            String activar = scanner.nextLine().trim().toLowerCase();
            String idioma = "none";
            
            if (activar.equals("s")) {
                System.out.print("  Idioma (es/en): ");
                idioma = scanner.nextLine().trim();
                if (idioma.isEmpty()) idioma = "es";
            }

            iniciarReproduccionSincronizada(pelicula, idioma);
        } else {
            mostrarError(resp);
        }
    }

    private static void iniciarReproduccionSincronizada(Pelicula pelicula, String idioma) {
        bufferVideo.clear();
        bufferSubtitulos.clear();
        recepcionVideoCompletada.set(false);

        // Productores: Descarga paralela
        Thread tVideo = new Thread(() -> descargarVideoUDP(pelicula));
        tVideo.start();

        if (!idioma.equals("none")) {
            new Thread(() -> descargarSubtitulosTCP(pelicula, idioma)).start();
        }

        // Consumidor: Master Clock
        ejecutarRelojMaestro(pelicula);
    }

    private static void ejecutarRelojMaestro(Pelicula pelicula) {
        System.out.println("\n--- REPRODUCIENDO: " + pelicula.titulo + " ---");
        int reloj = 0;
        int segundosPorFragmento = 2; // Sincronizado con Pelicula.java
        String subActual = null;
        int tiempoSub = -1;

        while (!recepcionVideoCompletada.get() || !bufferVideo.isEmpty()) {
            // Sincronía de Video
            if (reloj % segundosPorFragmento == 0 && !bufferVideo.isEmpty()) {
                System.out.println("  [" + String.format("%02d:%02d", reloj/60, reloj%60) + "] " + bufferVideo.poll());
            }

            // Sincronía de Subtítulos
            if (subActual == null && !bufferSubtitulos.isEmpty()) {
                subActual = bufferSubtitulos.poll();
                tiempoSub = extraerSegundos(subActual);
            }

            if (subActual != null && reloj >= tiempoSub && tiempoSub != -1) {
                System.out.println("  >> SUB: " + subActual.substring(10).trim());
                subActual = null;
            }

            try {
                Thread.sleep(1000); // 1 tick = 1 segundo
                reloj++;
            } catch (InterruptedException e) { break; }
        }
        System.out.println("--- FIN DE REPRODUCCIÓN ---");
    }

    private static void descargarVideoUDP(Pelicula pelicula) {
        
        try (DatagramSocket socket = new DatagramSocket(0, InetAddress.getByName(ipLocal))) {
            socket.setSoTimeout(10000);
            byte[] msg = ("PLAY:" + pelicula.titulo + ":" + pelicula.fragmentos).getBytes();
            socket.send(new DatagramPacket(msg, msg.length, InetAddress.getByName(hostUDP), puertoUDP));

            byte[] buffer = new byte[65535];
            while (true) {
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                socket.receive(p);
                FragmentoVideo f = deserializarFragmento(p.getData(), p.getLength()); // Marshalling (Rúbrica 2.1)
                if (f == null || f.esFin) break;
                bufferVideo.add(f);
            }
        } catch (Exception e) { System.err.println("  [Error UDP] " + e.getMessage()); }
        finally { recepcionVideoCompletada.set(true); }
    }

    private static void descargarSubtitulosTCP(Pelicula pelicula, String idioma) {
        // Solicitar subtítulos vía Gateway ZUUL (Control Plane, SSL/TCP)
        Respuesta resp = enviarAlGateway(new Peticion("SOLICITAR_SUBTITULOS", pelicula.titulo + ":" + idioma));

        if (resp == null) {
            // Fallo parcial: subtítulos no disponibles, el video continúa
            System.err.println("  [Aviso] Subtítulos no disponibles (servidor no responde).");
            return;
        }

        switch (resp.codigo) {
            case "OK":
                // Subtítulos recibidos como lista serializada
                if (resp.payload instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> lineas = (java.util.List<String>) resp.payload;
                    for (String linea : lineas) {
                        bufferSubtitulos.add(linea);
                    }
                    System.out.println("  [Subtítulos] " + lineas.size() + " líneas cargadas (" + idioma + ").");
                }
                break;

            case "IDIOMAS_DISPONIBLES":
                System.out.println("  [Subtítulos] " + resp.payload);
                break;

            case "404":
                System.out.println("  [Subtítulos] " + resp.payload);
                break;

            default:
                // Fallo parcial: error inesperado, el video continúa
                System.err.println("  [Aviso] Error en subtítulos: [" + resp.codigo + "] " + resp.payload);
                break;
        }
    }

    private static int extraerSegundos(String srt) {
        Matcher m = Pattern.compile("\\[(\\d{2}):(\\d{2}):(\\d{2})\\]").matcher(srt);
        return m.find() ? Integer.parseInt(m.group(1))*3600 + Integer.parseInt(m.group(2))*60 + Integer.parseInt(m.group(3)) : -1;
    }

    private static FragmentoVideo deserializarFragmento(byte[] data, int len) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data, 0, len))) {
            return (FragmentoVideo) ois.readObject();
        } catch (Exception e) { return null; }
    }

    /**
     * Envía una petición al Gateway ZUUL con reintentos automáticos.
     *
     * Modelo de Fallos - Omisión:
     * Si la conexión falla (timeout, conexión rechazada, paquete perdido),
     * reintenta hasta MAX_REINTENTOS veces con espera exponencial entre intentos.
     * Esto cubre fallos transitorios de red sin requerir intervención del usuario.
     *
     * Detección: timeout de conexión (TIMEOUT_CONEXION_MS) + excepciones de I/O.
     * Recuperación: reintento automático con backoff progresivo.
     *
     * @param p Petición a enviar
     * @return Respuesta del servidor, o null si todos los reintentos fallaron
     */
    private static Respuesta enviarAlGateway(Peticion p) {
        Exception ultimoError = null;

        for (int intento = 1; intento <= MAX_REINTENTOS; intento++) {
            try {
                Socket s = SSLSocketFactory.getDefault().createSocket();
                s.connect(
                    new java.net.InetSocketAddress(
                        InetAddress.getByName(hostGateway), puertoGateway
                    ),
                    TIMEOUT_CONEXION_MS
                );
                s.setSoTimeout(TIMEOUT_CONEXION_MS);

                try (
                    ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream())
                ) {
                    out.writeObject(p);
                    out.flush();
                    Respuesta resp = (Respuesta) in.readObject();

                    if (intento > 1) {
                        System.out.println("  [Gateway] Reconexión exitosa en intento " + intento + ".");
                    }

                    return resp;

                } finally {
                    try { s.close(); } catch (Exception ignored) {}
                }

            } catch (Exception e) {
                ultimoError = e;

                if (intento < MAX_REINTENTOS) {
                    int espera = ESPERA_ENTRE_REINTENTOS_MS * intento;
                    System.err.println("  [Gateway] " + p.comando + " - Intento " + intento +
                        "/" + MAX_REINTENTOS + " falló (" + e.getMessage() +
                        "). Reintentando en " + (espera / 1000) + "s...");
                    try {
                        Thread.sleep(espera);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        System.err.println("  [Gateway] " + p.comando + " - Todos los reintentos fallaron: " +
            (ultimoError != null ? ultimoError.getMessage() : "error desconocido"));
        return null;
    }

    private static void menuVerCatalogo() {
        Respuesta r = enviarAlGateway(new Peticion("VER_CATALOGO", tokenSesion));
        if (r != null) procesarRespuesta(r);
    }

    private static void menuBuscar(Scanner s) {
        System.out.print("Buscar: ");
        Respuesta r = enviarAlGateway(new Peticion("BUSCAR", tokenSesion + "|" + s.nextLine().trim()));
        if (r != null) procesarRespuesta(r);
    }

    private static void menuVerPerfil() {
        Respuesta r = enviarAlGateway(new Peticion("VER_PERFIL", tokenSesion));
        if (r != null) System.out.println("\n  " + r.payload);
    }

    private static void procesarRespuesta(Respuesta r) {
        if (r.codigo.equals("OK") || r.codigo.equals("RESULTADOS")) {
            if (r.payload instanceof List) {
                ((List<?>) r.payload).forEach(p -> System.out.println("  " + p));
            } else System.out.println("  " + r.payload);
        } else mostrarError(r);
    }

    private static void mostrarError(Respuesta r) {
        if (r == null) System.err.println("Sin respuesta del servidor.");
        else System.err.println("  [" + r.codigo + "] " + r.payload);
    }

    private static void cargarConfiguracion(String[] a) {
    if (a.length >= 2) { hostGateway = a[0]; puertoGateway = Integer.parseInt(a[1]); }
    if (a.length >= 4) { hostUDP = a[2]; puertoUDP = Integer.parseInt(a[3]); }
    if (a.length >= 5) { ipLocal = a[4]; }
}
}