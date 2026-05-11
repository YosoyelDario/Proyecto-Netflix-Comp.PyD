package servers;

import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;
import compartido.RepositorioPeliculas;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorCatalogo {
    private static final int PORT = 5000;
    private final RepositorioPeliculas repositorio = new RepositorioPeliculas();

    public static void main(String[] args) {
        new ServidorCatalogo().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("==================================================");
            System.out.println("[SISTEMA] Servidor de Catalogo (TCP) INICIADO");
            System.out.println("[SISTEMA] Escuchando en puerto: " + PORT);
            System.out.println("==================================================");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();
                System.out.println("\n[CONEXION] Nuevo cliente detectado -> IP: " + ipCliente);
                
                // Creación del hilo. Se nombra el hilo para seguimiento.
                System.out.println("\n[CONEXION] Solicitud de catálogo desde -> IP: " + ipCliente);
                
                // Adaptación: Usamos un Handler especializado solo en catálogo
                Thread hiloCliente = new Thread(new CatalogoHandler(clientSocket, repositorio, ipCliente));
                hiloCliente.start();
                hiloCliente.start();
            }
        } catch (IOException e) {
            System.err.println("[ERROR CRÍTICO] Fallo en Servidor Catalogo: " + e.getMessage());
        }
    }
}

/**
 * Gestiona únicamente la Función A (Catálogo) y el paso a la Función B (Streaming)
 */
class CatalogoHandler implements Runnable {
    private final Socket socket;
    private final RepositorioPeliculas repo;
    private final String ipCliente;

    public CatalogoHandler(Socket socket, RepositorioPeliculas repo, String ipCliente) {
        this.socket = socket;
        this.repo = repo;
        this.ipCliente = ipCliente;
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Object recibido;
            while ((recibido = in.readObject()) != null) {
                if (recibido instanceof Peticion) {
                    Peticion p = (Peticion) recibido;
                    String cmd = p.comando.toUpperCase();
                    
                    switch (cmd) {
                        case "VER_CATALOGO":
                            // Enviamos la lista completa de películas (Marshalling de objetos)
                            out.writeObject(new Respuesta("OK", repo.obtenerTodas()));
                            System.out.println("[" + nombreHilo + "] Catálogo enviado a " + ipCliente);
                            break;

                        case "ELEGIR_PELICULA":
                            Pelicula pelicula = repo.buscar(p.parametro);
                            if (pelicula != null) {
                                // Aquí enviamos la info técnica necesaria para el streaming UDP
                                out.writeObject(new Respuesta("STREAM_INFO", pelicula));
                                System.out.println("[" + nombreHilo + "] Derivación a UDP autorizada: " + pelicula.titulo);
                            } else {
                                out.writeObject(new Respuesta("ERROR", "Película no encontrada"));
                            }
                            break;
                            
                        case "SALIR":
                            return;
                    }
                }
                out.flush();
            }
        } catch (EOFException e) {
            // Desconexión normal del cliente
        } catch (Exception e) {
            System.err.println("[CATALOGO] Error de comunicación con " + ipCliente + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}


