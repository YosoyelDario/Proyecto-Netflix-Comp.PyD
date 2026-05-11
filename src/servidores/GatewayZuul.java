package servidores;

import compartido.Peticion;
import compartido.Respuesta;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Gateway ZUUL - Enrutador / Balanceador (Puerto 4000).
 *
 * Punto de entrada único para todos los clientes.
 * Provee Transparencia de Acceso y Ubicación: el cliente solo conoce
 * la IP de este Gateway y no necesita saber cuántos servidores hay
 * detrás ni dónde están ubicados.
 *
 * Enrutamiento:
 * - LOGIN, VALIDAR_TOKEN, PERFIL  ->  Servidor B (Autenticación)
 * - VER_CATALOGO, BUSCAR, ELEGIR_PELICULA, VER_PERFIL, SALIR  ->  Servidor A (Catálogo)
 * - SOLICITAR_SUBTITULOS  ->  Servidor C (Subtítulos)
 *
 * El streaming UDP va directo al cliente (Data Plane),
 * no pasa por este Gateway (Control Plane).
 */
public class GatewayZuul {
    private static int puerto = 4000;

    // Servidor A - Catálogo y Búsqueda
    private static String hostServidorA = "localhost";
    private static int puertoServidorA = 5000;

    // Servidor B - Perfiles y Autenticación
    private static String hostServidorB = "localhost";
    private static int puertoServidorB = 5100;

    // Servidor C - Subtítulos
    private static String hostServidorC = "localhost";
    private static int puertoServidorC = 7000;

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.keyStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "123456");
    System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        if (args.length >= 1) puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) hostServidorA = args[1];
        if (args.length >= 3) puertoServidorA = Integer.parseInt(args[2]);
        if (args.length >= 4) hostServidorB = args[3];
        if (args.length >= 5) puertoServidorB = Integer.parseInt(args[4]);
        if (args.length >= 6) hostServidorC = args[5];
        if (args.length >= 7) puertoServidorC = Integer.parseInt(args[6]);
        try (ServerSocket serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[GATEWAY] Gateway ZUUL iniciado");
            System.out.println("[GATEWAY] Escuchando en puerto: " + puerto);
            System.out.println("[GATEWAY] Servidor A (Catálogo):    " + hostServidorA + ":" + puertoServidorA);
            System.out.println("[GATEWAY] Servidor B (Auth):        " + hostServidorB + ":" + puertoServidorB);
            System.out.println("[GATEWAY] Servidor C (Subtítulos):  " + hostServidorC + ":" + puertoServidorC);
            System.out.println("==================================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();

                System.out.println("\n[GATEWAY] Nueva conexión: " + ipCliente);

                Thread hilo = new Thread(() -> manejarConexion(clientSocket, ipCliente));
                hilo.start();
            }

        } catch (IOException e) {
            System.err.println("[GATEWAY] Error crítico: " + e.getMessage());
        }
    }

    /**
     * Mantiene la conexión con el cliente y enruta cada petición
     * al servidor interno correspondiente.
     *
     * La conexión con el backend se abre por petición (no persistente),
     * lo que permite que los servidores internos se reinicien sin
     * afectar la conexión del cliente con el Gateway.
     */
    private static void manejarConexion(Socket clientSocket, String ipCliente) {
    String hilo = Thread.currentThread().getName();

    try (
        ObjectOutputStream outCliente = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inCliente = new ObjectInputStream(clientSocket.getInputStream())
    ) {
        Object recibido = inCliente.readObject(); // solo UNA lectura
        
        if (!(recibido instanceof Peticion)) {
            outCliente.writeObject(new Respuesta("400", "Solicitud inválida."));
            outCliente.flush();
            return;
        }

        Peticion peticion = (Peticion) recibido;
        peticion.ipOrigen = ipCliente;
        String comando = peticion.comando.toUpperCase();

        System.out.println("[" + hilo + "] " + ipCliente + " -> " + comando);

        switch (comando) {
            case "LOGIN":
            case "VALIDAR_TOKEN":
            case "PERFIL":
                reenviarPeticion(peticion, hostServidorB, puertoServidorB, outCliente, hilo);
                break;

            case "VER_CATALOGO":
            case "BUSCAR":
            case "VER_PERFIL":
            case "ELEGIR_PELICULA":
                reenviarPeticion(peticion, hostServidorA, puertoServidorA, outCliente, hilo);
                break;

            case "SOLICITAR_SUBTITULOS":
                reenviarPeticion(peticion, hostServidorC, puertoServidorC, outCliente, hilo);
                break;

            case "SALIR":
                System.out.println("[" + hilo + "] Cliente " + ipCliente + " cerró sesión.");
                reenviarPeticion(peticion, hostServidorA, puertoServidorA, outCliente, hilo);
                break;

            default:
                outCliente.writeObject(new Respuesta("400", "Comando no reconocido por el Gateway."));
                outCliente.flush();
        }

    } catch (java.net.SocketException e) {
            // Captura de cierre abrupto de red
            System.err.println("[" + hilo + "] Desconexión forzosa detectada (Connection reset): " + ipCliente);
        } catch (EOFException e) {
            System.out.println("[" + hilo + "] Desconexión normal: " + ipCliente);
        } catch (Exception e) {
            System.err.println("[" + hilo + "] Error con " + ipCliente + ": " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
}


    /**
     * Reenvía una petición al servidor interno y devuelve la respuesta al cliente.
     *
     * Abre una conexión TCP efímera al backend por cada petición.
     * Si el servidor destino no está disponible, retorna Error 503.
     */
    private static void reenviarPeticion(
            Peticion peticion,
            String host,
            int puerto,
            ObjectOutputStream outCliente,
            String hilo
    ) {
        try (
            Socket socketBackend = SSLSocketFactory.getDefault().createSocket(host, puerto);
    ObjectOutputStream outBackend = new ObjectOutputStream(socketBackend.getOutputStream());
            ObjectInputStream inBackend = new ObjectInputStream(socketBackend.getInputStream())
        ) {
            // Enviar petición al servidor interno
            outBackend.writeObject(peticion);
            outBackend.flush();

            // Recibir respuesta del servidor interno
            Object respuesta = inBackend.readObject();

            if (respuesta instanceof Respuesta) {
                outCliente.writeObject(respuesta);
            } else {
                outCliente.writeObject(new Respuesta("500", "Respuesta inválida del servidor interno."));
            }

            outCliente.flush();

            System.out.println("[" + hilo + "] Enrutado: " + peticion.comando + " -> " + host + ":" + puerto);

        } catch (Exception e) {
            try {
                outCliente.writeObject(new Respuesta("503", "Servicio no disponible: " + peticion.comando));
                outCliente.flush();
                System.err.println("[" + hilo + "] ERROR 503: No se pudo contactar " + host + ":" + puerto);
            } catch (IOException ex) {
                System.err.println("[" + hilo + "] Error enviando 503 al cliente: " + ex.getMessage());
            }
        }
    }
}