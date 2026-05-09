package servers;
import java.io.*;
import java.net.*;

public class SubtitleServer {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(7000)) {
            System.out.println("Servidor de subtitulos activo en puerto 7000...");
            while (true) {
                Socket cliente = server.accept();
                // Concurrencia, se aplica de un hilo por cliente aqui

                new Thread(() -> {
                    try (PrintWriter out = new PrintWriter(cliente.getOutputStream(), true)){
                        String[] subs = {"[Música]", "Buenas, bienvenido a Netfllix", "Disfruta la pelicula"};
                        for (String s : subs){
                            out.println(s);
                            Thread.sleep(2000); // SE SIMULA la sincronizacion con el video
                        }
                    } catch (Exception e) {
                        System.out.println("Conexión de subsitulos cerrada.");
                        }
                }).start();                
            }
        } catch (Exception e){
            e.printStackTrace();
        }    
    }
}
