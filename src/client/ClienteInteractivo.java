package client;
import compartido.Pelicula;
import compartido.Peticion;
import compartido.PeticionLogin;
import compartido.Respuesta;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/** 
 * Cliente interactivo de cmd para la interacción con el servidor de Netflix
 */

public class ClienteInteractivo {
    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int AUTH_PORT = 5001;

    public static void main(String[] args) {
        // Se utilizan Object Streams para el Marshalling
        try (Scanner scanner = new Scanner(System.in)) {
            
            System.out.println("--- INICIO DE SESIÓN ---");
            System.out.print("Usuario: ");
            String user = scanner.nextLine();
            System.out.print("Contraseña: ");
            String pass = scanner.nextLine();
            
            // Autenticación directa temporalmente al Servidor de Autenticación
            try (Socket authSocket = new Socket(HOST, AUTH_PORT);
                 ObjectOutputStream outAuth = new ObjectOutputStream(authSocket.getOutputStream());
                 ObjectInputStream inAuth = new ObjectInputStream(authSocket.getInputStream())) {
                 
                outAuth.writeObject(new PeticionLogin(user, pass));
                Respuesta authResp = (Respuesta) inAuth.readObject();
                if (!"OK".equals(authResp.codigo)) {
                    System.err.println("Autenticación fallida: " + authResp.payload);
                    return;
                }
                System.out.println("Autenticación exitosa! Token: " + authResp.payload);
            }

            // Conexión al servidor de catálogo
            try (Socket socketTCP = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socketTCP.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socketTCP.getInputStream())) {

            System.out.println("Conectado al Servidor de Control (Marshalling activo).");

            while (true) {
                System.out.println("\n--- NETFLIX INTERACTIVO ---");
                System.out.println("1. Ver Catálogo");
                System.out.println("2. Ver Perfil / Mi Lista");
                System.out.println("3. Reproducir Película");
                System.out.println("4. Salir");
                System.out.print("Selección: ");
                
                String opcion = scanner.nextLine();
                Peticion p = null;

                if (opcion.equals("1")) {
                    p = new Peticion("VER_CATALOGO", "");
                } else if (opcion.equals("2")) {
                    p = new Peticion("VER_PERFIL", user);
                } else if (opcion.equals("3")) {
                    System.out.print("Título de la película: ");
                    p = new Peticion("ELEGIR_PELICULA", scanner.nextLine());
                } else if (opcion.equals("4")) {
                    out.writeObject(new Peticion("SALIR", ""));
                    break;
                }

                if (p != null) {
                    if (opcion.equals("2")) {
                        // Temporalmente dirigimos la petición de perfil directo al Auth Server
                        try (Socket authSocket = new Socket(HOST, AUTH_PORT);
                             ObjectOutputStream outAuth = new ObjectOutputStream(authSocket.getOutputStream());
                             ObjectInputStream inAuth = new ObjectInputStream(authSocket.getInputStream())) {
                            outAuth.writeObject(p);
                            procesarRespuesta((Respuesta) inAuth.readObject());
                        }
                        continue;
                    }

                    out.writeObject(p); // Envío del objeto serializado
                    out.flush();
                    
                    // Recepción de la respuesta serializada
                    Respuesta resp = (Respuesta) in.readObject();
                    procesarRespuesta(resp);
                }
            }
            }
        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
        }
    }

    private static void procesarRespuesta(Respuesta resp) {
        switch (resp.codigo) {
            case "OK":
                System.out.println("-> Servidor: " + resp.payload);
                break;
            case "STREAM_INFO":
                Pelicula pelicula = (Pelicula) resp.payload;
                iniciarStreamingUDP(pelicula);
                break;
            case "ERROR":
                System.err.println("-> Error del Servidor: " + resp.payload);
                break;
        }
    }

    private static void iniciarStreamingUDP(Pelicula p) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress ip = InetAddress.getByName("localhost");
            // Payload para el servidor UDP
            String msg = "PLAY:" + p.titulo + ":" + p.fragmentos;
            byte[] buf = msg.getBytes();
            
            udpSocket.send(new DatagramPacket(buf, buf.length, ip, 6000));
            System.out.println("--- REPRODUCIENDO: " + p.titulo + " ---");

            byte[] bufferEntrada = new byte[1024];
            while (true) {
                DatagramPacket paquete = new DatagramPacket(bufferEntrada, bufferEntrada.length);
                udpSocket.receive(paquete);
                String data = new String(paquete.getData(), 0, paquete.getLength());
                
                if (data.equals("FIN_STREAMING")) break;
                System.out.println(data);
            }
            System.out.println("--- VIDEO FINALIZADO ---");
        } catch (Exception e) {
            System.err.println("Error en flujo UDP: " + e.getMessage());
        }
    }
}