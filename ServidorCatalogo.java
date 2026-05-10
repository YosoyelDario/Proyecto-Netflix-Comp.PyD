import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;

public class ServidorCatalogo {
    private static final int PORT = 5000;
    private final RepositorioPeliculas repositorio = new RepositorioPeliculas();

    public static void main(String[] args) {
        new ServidorCatalogo().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("==================================================");
            System.out.println("[SISTEMA] Servidor de Control (TCP) INICIADO");
            System.out.println("[SISTEMA] Escuchando en puerto: " + PORT);
            System.out.println("==================================================");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();
                System.out.println("\n[CONEXION] Nuevo cliente detectado -> IP: " + ipCliente);
                
                // Creación del hilo. Se nombra el hilo para seguimiento.
                Thread hiloCliente = new Thread(new Handler(clientSocket, repositorio, ipCliente));
                hiloCliente.start();
            }
        } catch (IOException e) {
            System.err.println("[ERROR CRÍTICO] Fallo en Servidor TCP: " + e.getMessage());
        }
    }
}

/**
 * Capa de Almacenamiento (Simulación de base de datos)
 */
class RepositorioPeliculas {
    private final Map<String, Pelicula> db = new HashMap<>();
    private final Map<String, String> usuarios = new HashMap<>();

    public RepositorioPeliculas() {
        db.put("matrix", new Pelicula("Matrix", 10));
        db.put("inception", new Pelicula("Inception", 15));
        db.put("interstellar", new Pelicula("Interstellar", 7));
        
        usuarios.put("vicente", "Plan Premium - Chile");
    }

    public synchronized List<Pelicula> getLista() {
        return new ArrayList<>(db.values());
    }

    public synchronized Pelicula buscar(String titulo) {
        return db.get(titulo.toLowerCase());
    }

    public synchronized String getPerfil(String user) {
        return usuarios.getOrDefault(user.toLowerCase(), "Usuario inexistente");
    }
}

/**
 * Gestor de hilos con Marshalling de objetos y Trazas
 */
class Handler implements Runnable {
    private final Socket socket;
    private final RepositorioPeliculas repo;
    private final String ipCliente;

    public Handler(Socket socket, RepositorioPeliculas repo, String ipCliente) {
        this.socket = socket;
        this.repo = repo;
        this.ipCliente = ipCliente;
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        System.out.println("[" + nombreHilo + "] Asignado para procesar peticiones de: " + ipCliente);

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Object recibido;
            while ((recibido = in.readObject()) != null) {
                if (!(recibido instanceof Peticion)) continue;
                
                Peticion p = (Peticion) recibido;
                String cmd = p.comando.toUpperCase();
                
                System.out.println("[" + nombreHilo + "] " + ipCliente + " ejecutó: " + cmd + (p.parametro.isEmpty() ? "" : " -> '" + p.parametro + "'"));

                switch (cmd) {
                    case "VER_CATALOGO":
                        out.writeObject(new Respuesta("OK", repo.getLista()));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada (Catálogo).");
                        break;

                    case "VER_PERFIL":
                        out.writeObject(new Respuesta("OK", repo.getPerfil(p.parametro)));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada (Perfil).");
                        break;

                    case "ELEGIR_PELICULA":
                        Pelicula pelicula = repo.buscar(p.parametro);
                        if (pelicula != null) {
                            out.writeObject(new Respuesta("STREAM_INFO", pelicula));
                            System.out.println("[" + nombreHilo + "] Derivación a Servidor Streaming (UDP) autorizada.");
                        } else {
                            out.writeObject(new Respuesta("ERROR", "Película no encontrada"));
                            System.out.println("[" + nombreHilo + "] Error de búsqueda: Película no existe.");
                        }
                        break;

                    case "SALIR":
                        System.out.println("[" + nombreHilo + "] Cliente solicitó cierre de sesión (SALIR).");
                        return;
                }
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println("[" + nombreHilo + "] Desconexión normal (EOF) de: " + ipCliente);
        } catch (Exception e) {
            System.err.println("[" + nombreHilo + "] Error de comunicación con " + ipCliente + " -> " + e.getMessage());
        } finally {
            try { 
                socket.close(); 
                System.out.println("[" + nombreHilo + "] Recursos liberados y socket cerrado para: " + ipCliente);
            } catch (IOException ignored) {}
        }
    }
}