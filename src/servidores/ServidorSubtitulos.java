package servidores;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Servidor C - Subtítulos TCP (Puerto 7000).
 *
 * Responsabilidades:
 * - Recibir solicitudes de subtítulos por TCP (movieID, idioma)
 * - Buscar archivo .srt correspondiente en BD Subtítulos
 * - Enviar subtítulos línea por línea sincronizados con el video
 * - Si el idioma no existe, ofrecer idiomas disponibles
 *
 * Comunicación:
 * - TCP con Clientes (directo, paralelo al streaming UDP)
 *
 * Fallo independiente: si este servidor cae, el streaming
 * de video continúa sin subtítulos.
 */
public class ServidorSubtitulos {
    private static int puerto = 7000;
    private static String rutaSubtitulos = "data/subtitulos";

    public static void main(String[] args) {
        if (args.length >= 1) puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) rutaSubtitulos = args[1];

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[SUBTITULOS] Servidor de Subtítulos TCP iniciado");
            System.out.println("[SUBTITULOS] Escuchando en puerto: " + puerto);
            System.out.println("[SUBTITULOS] BD Subtítulos: " + rutaSubtitulos);
            System.out.println("[SUBTITULOS] Archivos disponibles:");
            listarArchivosDisponibles();
            System.out.println("==================================================");

            while (true) {
                Socket cliente = serverSocket.accept();
                String ipCliente = cliente.getInetAddress().getHostAddress();

                System.out.println("\n[SUBTITULOS] Cliente conectado: " + ipCliente);

                Thread hiloCliente = new Thread(() -> atenderCliente(cliente, ipCliente));
                hiloCliente.start();
            }

        } catch (Exception e) {
            System.err.println("[SUBTITULOS] Error crítico: " + e.getMessage());
        }
    }

    /**
     * Atiende a un cliente: recibe la solicitud (película:idioma),
     * busca el archivo .srt y envía los subtítulos.
     */
    private static void atenderCliente(Socket cliente, String ipCliente) {
        String hilo = Thread.currentThread().getName();

        try (
            BufferedReader inCliente = new BufferedReader(
                new InputStreamReader(cliente.getInputStream())
            );
            PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)
        ) {
            // Leer solicitud del cliente: "pelicula:idioma"
            String solicitud = inCliente.readLine();

            if (solicitud == null || solicitud.trim().isEmpty()) {
                out.println("ERROR:Solicitud vacía");
                out.println("FIN_SUBTITULOS");
                return;
            }

            System.out.println("[" + hilo + "] Solicitud: " + solicitud);

            String[] partes = solicitud.split(":", 2);
            String pelicula = partes[0].trim().toLowerCase().replaceAll("\\s+", "_");
            String idioma = partes.length > 1 ? partes[1].trim().toLowerCase() : "es";

            // Buscar archivo .srt
            String nombreArchivo = pelicula + "_" + idioma + ".srt";
            File archivo = new File(rutaSubtitulos, nombreArchivo);

            if (archivo.exists()) {
                // Subtítulos encontrados -> enviar línea por línea
                System.out.println("[" + hilo + "] Enviando: " + nombreArchivo);
                enviarSubtitulos(archivo, out, hilo);

            } else {
                // Idioma no disponible -> buscar alternativas
                List<String> idiomasDisponibles = buscarIdiomasDisponibles(pelicula);

                if (idiomasDisponibles.isEmpty()) {
                    out.println("ERROR:No hay subtítulos disponibles para '" + pelicula + "'");
                    System.out.println("[" + hilo + "] Sin subtítulos para: " + pelicula);
                } else {
                    out.println("IDIOMA_NO_DISPONIBLE:Idiomas disponibles: " + String.join(", ", idiomasDisponibles));
                    System.out.println("[" + hilo + "] Idioma '" + idioma + "' no disponible. Disponibles: " + idiomasDisponibles);
                }

                out.println("FIN_SUBTITULOS");
            }

        } catch (Exception e) {
            System.err.println("[" + hilo + "] Error con " + ipCliente + ": " + e.getMessage());

        } finally {
            try {
                cliente.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Lee el archivo .srt y envía cada línea con delay para
     * sincronizar con la reproducción del video.
     */
    private static void enviarSubtitulos(File archivo, PrintWriter out, String hilo) {
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int contador = 0;

            while ((linea = br.readLine()) != null) {
                linea = linea.trim();

                if (linea.isEmpty()) {
                    continue;
                }

                out.println(linea);
                contador++;

                // Sincronizar: un subtítulo cada 2 segundos
                //Thread.sleep(2000);
            }

            out.println("FIN_SUBTITULOS");
            System.out.println("[" + hilo + "] Subtítulos finalizados (" + contador + " líneas).");

        } catch (Exception e) {
            System.err.println("[" + hilo + "] Error leyendo .srt: " + e.getMessage());
            out.println("ERROR:Error interno al cargar subtítulos");
            out.println("FIN_SUBTITULOS");
        }
    }

    /**
     * Busca qué idiomas están disponibles para una película.
     * Escanea los archivos en la carpeta de subtítulos.
     */
    private static List<String> buscarIdiomasDisponibles(String pelicula) {
        List<String> idiomas = new ArrayList<>();

        File carpeta = new File(rutaSubtitulos);

        if (!carpeta.exists() || !carpeta.isDirectory()) {
            return idiomas;
        }

        File[] archivos = carpeta.listFiles();

        if (archivos == null) {
            return idiomas;
        }

        for (File f : archivos) {
            String nombre = f.getName();

            if (nombre.startsWith(pelicula + "_") && nombre.endsWith(".srt")) {
                // Extraer idioma: "matrix_es.srt" -> "es"
                String idioma = nombre
                    .replace(pelicula + "_", "")
                    .replace(".srt", "");
                idiomas.add(idioma);
            }
        }

        return idiomas;
    }

    /**
     * Lista los archivos .srt disponibles al iniciar el servidor.
     */
    private static void listarArchivosDisponibles() {
        File carpeta = new File(rutaSubtitulos);

        if (!carpeta.exists() || !carpeta.isDirectory()) {
            System.out.println("[SUBTITULOS]   (carpeta no encontrada: " + rutaSubtitulos + ")");
            return;
        }

        File[] archivos = carpeta.listFiles((dir, name) -> name.endsWith(".srt"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("[SUBTITULOS]   (sin archivos .srt)");
            return;
        }

        for (File f : archivos) {
            System.out.println("[SUBTITULOS]   -> " + f.getName());
        }
    }
}