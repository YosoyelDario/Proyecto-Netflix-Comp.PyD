package direccionamiento;
import compartido.ConfigObj;
import java.io.*;
import java.net.*;


public class DireccionamientoServer {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(5000)) {
            System.out.println("Servicio de direccionamiento escuchando en el puerto  5000...");
            while (true) {
                Socket cliente = server.accept();
                ObjectOutputStream out = new ObjectOutputStream(cliente.getOutputStream());

                // Se simula que se eligen los mejores nodos
                ConfigObj config = new ConfigObj("localhost", 6000, "localhost", 7000);

                out.writeObject(config); //Esto de aqui cumple con el Marshalling
                cliente.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }    
}
