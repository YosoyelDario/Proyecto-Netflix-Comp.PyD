package servidores;

import compartido.Peticion;
import compartido.Respuesta;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLServerSocketFactory;


public class ServidorSubtitulos {
    private static int puerto = 7000;
    private static String rutaSubtitulos = "data/subtitulos";

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.keyStore", "data/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        if (args.length >= 1) puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) rutaSubtitulos = args[1];

        try (ServerSocket serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[SUBTITULOS] Servidor de Subtítulos SSL/TCP iniciado");
            System.out.println("[SUBTITULOS] Escuchando en puerto: " + puerto);
            System.out.println("[SUBTITULOS] BD Subtítulos: " + rutaSubtitulos);
            System.out.println("[SUBTITULOS] Archivos disponibles:");
            listarArchivosDisponibles();
            System.out.println("==================================================");

            while (true) {
                Socket cliente = serverSocket.accept();
                String ipCliente = cliente.getInetAddress().getHostAddress();

                System.out.println("\n[SUBTITULOS] Conexión desde: " + ipCliente);

                Thread hiloCliente = new Thread(() -> atenderPeticion(cliente, ipCliente));
                hiloCliente.start();
            }

        } catch (Exception e) {
            System.err.println("[SUBTITULOS] Error crítico: " + e.getMessage());
        }
    }

    private static void atenderPeticion(Socket cliente, String ipCliente) {
        String hilo = Thread.currentThread().getName();

        try (
            ObjectOutputStream out = new ObjectOutputStream(cliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(cliente.getInputStream())
        ) {
            Object recibido = in.readObject();

            if (!(recibido instanceof Peticion)) {
                out.writeObject(new Respuesta("400", "Petición inválida."));
                out.flush();
                return;
            }

            Peticion peticion = (Peticion) recibido;
            String comando = peticion.comando.toUpperCase();

            System.out.println("[" + hilo + "] " + peticion.ipOrigen + " (vía Gateway) -> " + comando);

            if (!comando.equals("SOLICITAR_SUBTITULOS")) {
                out.writeObject(new Respuesta("400", "Comando no soportado por subtítulos: " + comando));
                out.flush();
                return;
            }

            String parametro = peticion.parametro;
            if (parametro == null || parametro.trim().isEmpty()) {
                out.writeObject(new Respuesta("400", "Solicitud vacía."));
                out.flush();
                return;
            }

            String[] partes = parametro.split(":", 2);
            String pelicula = partes[0].trim().toLowerCase().replaceAll("\\s+", "_");
            String idioma = partes.length > 1 ? partes[1].trim().toLowerCase() : "es";

            System.out.println("[" + hilo + "] Buscando: " + pelicula + "_" + idioma + ".srt");

            String nombreArchivo = pelicula + "_" + idioma + ".srt";
            File archivo = new File(rutaSubtitulos, nombreArchivo);

            if (archivo.exists()) {
                ArrayList<String> lineas = leerSubtitulos(archivo);
                out.writeObject(new Respuesta("OK", lineas));
                out.flush();
                System.out.println("[" + hilo + "] Enviados: " + nombreArchivo + " (" + lineas.size() + " líneas)");

            } else {
                List<String> idiomasDisponibles = buscarIdiomasDisponibles(pelicula);

                if (idiomasDisponibles.isEmpty()) {
                    out.writeObject(new Respuesta("404", "No hay subtítulos disponibles para '" + pelicula + "'"));
                    System.out.println("[" + hilo + "] Sin subtítulos para: " + pelicula);
                } else {
                    out.writeObject(new Respuesta("IDIOMAS_DISPONIBLES",
                        "Idioma '" + idioma + "' no disponible. Disponibles: " + String.join(", ", idiomasDisponibles)));
                    System.out.println("[" + hilo + "] Idioma '" + idioma + "' no encontrado. Disponibles: " + idiomasDisponibles);
                }
                out.flush();
            }

        } catch (Exception e) {
            System.err.println("[" + hilo + "] Error con " + ipCliente + ": " + e.getMessage());

        } finally {
            try {
                cliente.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static ArrayList<String> leerSubtitulos(File archivo) {
        ArrayList<String> lineas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    lineas.add(linea);
                }
            }

        } catch (Exception e) {
            System.err.println("[SUBTITULOS] Error leyendo .srt: " + e.getMessage());
        }

        return lineas;
    }

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
                String idioma = nombre
                    .replace(pelicula + "_", "")
                    .replace(".srt", "");
                idiomas.add(idioma);
            }
        }

        return idiomas;
    }

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
