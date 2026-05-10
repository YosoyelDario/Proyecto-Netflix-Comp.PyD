package servers;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoServer {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(6000)){
            System.out.println("Server de Video en puerto 6000...");
            while (true) {
                Socket cliente = server.accept();
                // crear hilo dedicado
                new Thread(() -> {
                    try{
                        PrintWriter out = new PrintWriter(cliente.getOutputStream(), true);
                        out.println("Enviando video chunk 1");
                        cliente.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }).start(); 
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
