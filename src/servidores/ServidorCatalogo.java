package servidores;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor A - Catálogo y Búsqueda (Puerto 5000).
 *
 * Recibe conexiones TCP del Gateway ZUUL (o directas del cliente).
 * Cada conexión trae una sola petición que se procesa en un hilo
 * independiente (concurrencia: un hilo por petición).
 *
 * Comunicación:
 * - TCP desde Gateway ZUUL (o cliente directo)
 * - TCP hacia Servidor B (Autenticación) para validar tokens
 * - Lee BD Metadatos desde peliculas.txt
 */
public class ServidorCatalogo {
    private int puerto = 5000;

    private String hostAuth = "localhost";
    private int puertoAuth = 5100;
    private String rutaBDPeliculas = "data/peliculas.txt";

    private RepositorioPeliculas repositorio;

    public static void main(String[] args) {
        ServidorCatalogo servidor = new ServidorCatalogo();

        if (args.length >= 1) servidor.puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) servidor.hostAuth = args[1];
        if (args.length >= 3) servidor.puertoAuth = Integer.parseInt(args[2]);
        if (args.length >= 4) servidor.rutaBDPeliculas = args[3];

        servidor.repositorio = new RepositorioPeliculas(servidor.rutaBDPeliculas);
        servidor.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[CATALOGO] Servidor A - Catálogo y Búsqueda");
            System.out.println("[CATALOGO] Escuchando en puerto: " + puerto);
            System.out.println("[CATALOGO] Servidor B (Auth): " + hostAuth + ":" + puertoAuth);
            System.out.println("[CATALOGO] BD Películas: " + rutaBDPeliculas);
            System.out.println("==================================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();

                // Un hilo por petición recibida (concurrencia)
                Thread hiloCliente = new Thread(
                    new Handler(clientSocket, repositorio, ipCliente, hostAuth, puertoAuth)
                );

                hiloCliente.start();
            }

        } catch (IOException e) {
            System.err.println("[CATALOGO] Error crítico: " + e.getMessage());
        }
    }
}