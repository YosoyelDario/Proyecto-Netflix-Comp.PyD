package client;
import compartido.ConfigObj;
import java.io.*;
import java.net.*;


public class ClienteNetflix {
    public static void main(String[] args) {
        try {
            // Resolución de ubicación
            Socket socketSteering = new Socket("localhost", 5000);
            ObjectInputStream in = new ObjectInputStream(socketSteering.getInputStream());
            ConfigObj config = (ConfigObj) in.readObject();
            socketSteering.close();

            System.out.println("Conectando a Video en: " + config.puertoVideo);
            
            //  Concurrencia (Hilos para video y subs)
            new Thread(() -> conectarNodo(config.puertoVideo, "VIDEO")).start();
            new Thread(() -> conectarNodo(config.puertoSub, "SUBS")).start();

        } catch (Exception e) { System.out.println("Fallo detectado: " + e.getMessage()); }
    }

    private static void conectarNodo(int puerto, String tipo) {
        try (Socket s = new Socket("localhost", puerto);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            System.out.println("Recibiendo " + tipo + ": " + in.readLine());
        } catch (Exception e) { System.out.println("Error en " + tipo + " (Fallo Independiente)"); }
    }
}
