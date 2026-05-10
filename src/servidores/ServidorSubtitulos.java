package servidores;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorSubtitulos {
    private static int puerto = 7000;

    public static void main(String[] args) {
        if (args.length >= 1) {
            puerto = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor de Subtítulos TCP iniciado. Puerto: " + puerto);

            while (true) {
                Socket cliente = serverSocket.accept();
                String ipCliente = cliente.getInetAddress().getHostAddress();

                System.out.println("[SUBTITULOS] Cliente conectado: " + ipCliente);

                Thread hiloCliente = new Thread(() -> atenderCliente(cliente, ipCliente));
                hiloCliente.start();
            }

        } catch (Exception e) {
            System.err.println("[SUBTITULOS] Error crítico: " + e.getMessage());
        }
    }

    private static void atenderCliente(Socket cliente, String ipCliente) {
        String nombreHilo = Thread.currentThread().getName();

        try (PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)) {
            System.out.println("[" + nombreHilo + "] Enviando subtítulos a: " + ipCliente);

            String[] subtitulos = {
                "[00:00] ♪ Música de introducción ♪",
                "[00:02] Bienvenido a Netflix distribuido.",
                "[00:04] El contenido se está reproduciendo desde un servidor remoto.",
                "[00:06] Los subtítulos viajan desde un proceso independiente.",
                "[00:08] Si este servidor falla, el video puede continuar.",
                "FIN_SUBTITULOS"
            };

            for (String linea : subtitulos) {
                out.println(linea);

                if (!linea.equals("FIN_SUBTITULOS")) {
                    Thread.sleep(2000);
                }
            }

            System.out.println("[" + nombreHilo + "] Subtítulos finalizados para: " + ipCliente);

        } catch (Exception e) {
            System.err.println("[" + nombreHilo + "] Conexión de subtítulos cerrada: " + e.getMessage());
        } finally {
            try {
                cliente.close();
            } catch (Exception ignored) {
            }
        }
    }
}