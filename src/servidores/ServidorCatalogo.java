package servidores;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLServerSocketFactory;
public class ServidorCatalogo {
    private int puerto = 5000;

    private String hostAuth = "localhost";
    private int puertoAuth = 5100;
    private String rutaBDPeliculas = "data/peliculas.txt";

    private RepositorioPeliculas repositorio;

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.keyStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "123456");
    System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        ServidorCatalogo servidor = new ServidorCatalogo();

        if (args.length >= 1) servidor.puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) servidor.hostAuth = args[1];
        if (args.length >= 3) servidor.puertoAuth = Integer.parseInt(args[2]);
        if (args.length >= 4) servidor.rutaBDPeliculas = args[3];

        servidor.repositorio = new RepositorioPeliculas(servidor.rutaBDPeliculas);
        servidor.start();
    }

    public void start() {
        
        try (ServerSocket serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[CATALOGO] Servidor A - Catálogo y Búsqueda");
            System.out.println("[CATALOGO] Escuchando en puerto: " + puerto);
            System.out.println("[CATALOGO] Servidor B (Auth): " + hostAuth + ":" + puertoAuth);
            System.out.println("[CATALOGO] BD Películas: " + rutaBDPeliculas);
            System.out.println("==================================================");

            ExecutorService pool = Executors.newFixedThreadPool(20);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();

                pool.execute(
        new Handler(clientSocket, repositorio, ipCliente, hostAuth, puertoAuth)
    );
            }

        } catch (IOException e) {
            System.err.println("[CATALOGO] Error crítico: " + e.getMessage());
        }
    }
}