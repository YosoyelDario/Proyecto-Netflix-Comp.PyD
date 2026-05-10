
package servers;
import compartido.*;
import compartido.RepositorioUsuarios;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Servidor de Autenticación, Gestiona Incio de Sesión y Perfil de Usuario (TCP), Gestión de Usuarios y Perfiles (Capa de Almacenamiento)
 */
public class ServidorAutenticacion {
    private static final int PORT = 5001; // puerto dedicado para autenticación
    private final RepositorioUsuarios repositorio = new RepositorioUsuarios();

    public static void main(String[] args) {
        new ServidorAutenticacion().start();
    }
    // Método principal para iniciar el servidor de autenticación
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("==================================================");
            System.out.println("[SISTEMA] Servidor de Autenticación (TCP) INICIADO");
            System.out.println("[SISTEMA] Escuchando en puerto: " + PORT);
            System.out.println("==================================================");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ipCliente = clientSocket.getInetAddress().getHostAddress();
                System.out.println("\n[CONEXION] Nuevo cliente detectado -> IP: " + ipCliente);
                
                Thread hiloCliente = new Thread(new AuthHandler(clientSocket, repositorio, ipCliente));
                hiloCliente.start();
            }
        } catch (IOException e) {
            System.err.println("[ERROR CRÍTICO] Fallo en Servidor Autenticación: " + e.getMessage());
        }
    }
}

/*
 * Handler para cada cliente que se conecta al Servidor de Autenticación
 * Gestiona el proceso de login y consultas relacionadas con el perfil del usuario.
 */
class AuthHandler implements Runnable {
    private final Socket socket;
    private final RepositorioUsuarios repo;
    private final String ipCliente;

    public AuthHandler(Socket socket, RepositorioUsuarios repo, String ipCliente) {
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
                
                // 1. Manejo de Logins
                if (recibido instanceof PeticionLogin) {
                    PeticionLogin p = (PeticionLogin) recibido;
                    System.out.println("[" + nombreHilo + "] Petición de Login de: " + p.usuario);
                    
                    if (repo.validarLogin(p.usuario, p.password)) {
                        out.writeObject(new Respuesta("OK", "Token-Sesion-12345"));
                        System.out.println("[" + nombreHilo + "] Login EXITOSO para: " + p.usuario);
                    } else {
                        out.writeObject(new Respuesta("ERROR", "Credenciales incorrectas"));
                        System.out.println("[" + nombreHilo + "] Login FALLIDO para: " + p.usuario);
                    }
                }
                // 2. Manejo de peticiones de sistema
                else if (recibido instanceof Peticion) {
                    Peticion p = (Peticion) recibido;
                    String cmd = p.comando.toUpperCase();
                    System.out.println("[" + nombreHilo + "] " + ipCliente + " ejecutó: " + cmd);
                    
                    switch (cmd) {
                        case "VER_PERFIL":
                        case "VER_MI_LISTA":
                            Usuario u = repo.getPerfil(p.parametro);
                            if (u != null) {
                                out.writeObject(new Respuesta("OK", u));
                                System.out.println("[" + nombreHilo + "] Respuesta enviada (Perfil/Mi Lista).");
                            } else {
                                out.writeObject(new Respuesta("ERROR", "Usuario no encontrado"));
                            }
                            break;
                            
                        case "SALIR":
                            System.out.println("[" + nombreHilo + "] Cliente solicitó cierre de sesión (SALIR).");
                            return; // Sale del while y termina el hilo
                    }
                }
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println("[" + nombreHilo + "] Desconexión normal (EOF) de: " + ipCliente);
        } catch (Exception e) {
            System.err.println("[" + nombreHilo + "] Error de comunicación con " + ipCliente + " -> " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}