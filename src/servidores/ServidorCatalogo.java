package servidores;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorCatalogo {
    private int puerto = 5000;

    private String hostAuth = "localhost";
    private int puertoAuth = 5100;

    private final RepositorioPeliculas repositorio = new RepositorioPeliculas();

    public static void main(String[] args) {
        ServidorCatalogo servidor = new ServidorCatalogo();

        if (args.length >= 1) {
            servidor.puerto = Integer.parseInt(args[0]);
        }

        if (args.length >= 2) {
            servidor.hostAuth = args[1];
        }

        if (args.length >= 3) {
            servidor.puertoAuth = Integer.parseInt(args[2]);
        }

        servidor.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[SISTEMA] Servidor de Catálogo TCP iniciado");
            System.out.println("[SISTEMA] Escuchando en puerto: " + puerto);
            System.out.println("[SISTEMA] Servidor Auth configurado en: " + hostAuth + ":" + puertoAuth);
            System.out.println("==================================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();

                System.out.println("\n[CONEXION] Nuevo cliente detectado -> IP: " + ipCliente);

                Thread hiloCliente = new Thread(
                    new Handler(clientSocket, repositorio, ipCliente, hostAuth, puertoAuth)
                );

                hiloCliente.start();
            }

        } catch (IOException e) {
            System.err.println("[ERROR CRÍTICO] Fallo en Servidor TCP: " + e.getMessage());
        }
    }
}