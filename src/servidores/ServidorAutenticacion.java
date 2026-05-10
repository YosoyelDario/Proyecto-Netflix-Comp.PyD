package servidores;

import compartido.Peticion;
import compartido.Respuesta;
import compartido.Usuario;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorAutenticacion {
    private static int puerto = 5100;
    private static final RepositorioUsuarios repoUsuarios = new RepositorioUsuarios();

    public static void main(String[] args) {
        if (args.length >= 1) {
            puerto = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[AUTH] Servidor de Autenticación iniciado");
            System.out.println("[AUTH] Escuchando en puerto: " + puerto);
            System.out.println("==================================================");

            while (true) {
                Socket socketCliente = serverSocket.accept();

                Thread hilo = new Thread(() -> atenderSolicitud(socketCliente));
                hilo.start();
            }

        } catch (Exception e) {
            System.err.println("[AUTH] Error crítico: " + e.getMessage());
        }
    }

    private static void atenderSolicitud(Socket socketCliente) {
        String nombreHilo = Thread.currentThread().getName();

        try (
            ObjectOutputStream out = new ObjectOutputStream(socketCliente.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socketCliente.getInputStream())
        ) {
            Object recibido = in.readObject();

            if (!(recibido instanceof Peticion)) {
                out.writeObject(new Respuesta("ERROR", "Solicitud inválida."));
                out.flush();
                return;
            }

            Peticion peticion = (Peticion) recibido;
            String comando = peticion.comando.toUpperCase();

            System.out.println("[" + nombreHilo + "] Solicitud recibida: " + comando);

            if (comando.equals("LOGIN")) {
                procesarLogin(peticion, out);
            } else {
                out.writeObject(new Respuesta("ERROR", "Comando no soportado por autenticación."));
                out.flush();
            }

        } catch (Exception e) {
            System.err.println("[" + nombreHilo + "] Error en autenticación: " + e.getMessage());

        } finally {
            try {
                socketCliente.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void procesarLogin(Peticion peticion, ObjectOutputStream out) throws Exception {
        String[] partes = peticion.parametro.split(":");

        if (partes.length < 2) {
            out.writeObject(new Respuesta("ERROR", "Formato de login inválido."));
            out.flush();
            return;
        }

        String username = partes[0];
        String password = partes[1];

        Usuario usuario = repoUsuarios.autenticar(username, password);

        if (usuario == null) {
            out.writeObject(new Respuesta("ERROR", "Credenciales incorrectas."));
            out.flush();
            return;
        }

        out.writeObject(new Respuesta("LOGIN_OK", usuario));
        out.flush();

        System.out.println("[AUTH] Usuario autenticado: " + usuario.username);
    }
}