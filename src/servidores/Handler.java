package servidores;

import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;
import compartido.Usuario;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Handler implements Runnable {
    private final Socket socket;
    private final RepositorioPeliculas repo;
    private final String ipCliente;

    private Usuario usuarioActual;

    public Handler(Socket socket, RepositorioPeliculas repo, String ipCliente) {
        this.socket = socket;
        this.repo = repo;
        this.ipCliente = ipCliente;
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        System.out.println("[" + nombreHilo + "] Asignado para procesar peticiones de: " + ipCliente);

        try (
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            Object recibido;

            while ((recibido = in.readObject()) != null) {
                if (!(recibido instanceof Peticion)) {
                    continue;
                }

                Peticion peticion = (Peticion) recibido;
                String comando = peticion.comando.toUpperCase();

                System.out.println(
                    "[" + nombreHilo + "] " + ipCliente + " ejecutó: " +
                    comando +
                    (peticion.parametro.isEmpty() ? "" : " -> '" + peticion.parametro + "'")
                );

                switch (comando) {
                    case "LOGIN":
                        procesarLogin(peticion, out);
                        break;

                    case "VER_CATALOGO":
                        if (!estaAutenticado(out)) {
                            break;
                        }

                        out.writeObject(new Respuesta("OK", repo.getLista()));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada: Catálogo.");
                        break;

                    case "VER_PERFIL":
                        if (!estaAutenticado(out)) {
                            break;
                        }

                        out.writeObject(new Respuesta("OK", usuarioActual.toString()));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada: Perfil.");
                        break;

                    case "ELEGIR_PELICULA":
                        if (!estaAutenticado(out)) {
                            break;
                        }

                        if (!tieneSuscripcionActiva(out)) {
                            break;
                        }

                        Pelicula pelicula = repo.buscar(peticion.parametro);

                        if (pelicula != null) {
                            out.writeObject(new Respuesta("STREAM_INFO", pelicula));
                            System.out.println("[" + nombreHilo + "] Película autorizada para streaming UDP.");
                        } else {
                            out.writeObject(new Respuesta("ERROR", "Película no encontrada."));
                            System.out.println("[" + nombreHilo + "] Error: Película no existe.");
                        }
                        break;

                    case "SALIR":
                        System.out.println("[" + nombreHilo + "] Cliente solicitó cierre de sesión.");
                        return;

                    default:
                        out.writeObject(new Respuesta("ERROR", "Comando no reconocido."));
                        System.out.println("[" + nombreHilo + "] Comando no reconocido: " + comando);
                        break;
                }

                out.flush();
            }

        } catch (EOFException e) {
            System.out.println("[" + nombreHilo + "] Desconexión normal de: " + ipCliente);

        } catch (Exception e) {
            System.err.println(
                "[" + nombreHilo + "] Error de comunicación con " +
                ipCliente + " -> " + e.getMessage()
            );

        } finally {
            try {
                socket.close();
                System.out.println("[" + nombreHilo + "] Socket cerrado para: " + ipCliente);
            } catch (IOException ignored) {
            }
        }
    }

    private void procesarLogin(Peticion peticion, ObjectOutputStream out) throws IOException {
        String[] partes = peticion.parametro.split(":");

        if (partes.length < 2) {
            out.writeObject(new Respuesta("ERROR", "Formato de login inválido."));
            return;
        }

        String username = partes[0];
        String password = partes[1];

        Usuario usuario = repo.autenticar(username, password);

        if (usuario == null) {
            out.writeObject(new Respuesta("ERROR", "Credenciales incorrectas."));
            return;
        }

        usuarioActual = usuario;

        if (!usuarioActual.suscripcionActiva) {
            out.writeObject(new Respuesta("LOGIN_OK", "Login correcto, pero la suscripción está inactiva."));
            return;
        }

        out.writeObject(new Respuesta("LOGIN_OK", "Bienvenido " + usuarioActual.username + ". " + usuarioActual.plan));
    }

    private boolean estaAutenticado(ObjectOutputStream out) throws IOException {
        if (usuarioActual == null) {
            out.writeObject(new Respuesta("ERROR", "Debe iniciar sesión antes de usar el sistema."));
            return false;
        }

        return true;
    }

    private boolean tieneSuscripcionActiva(ObjectOutputStream out) throws IOException {
        if (!usuarioActual.suscripcionActiva) {
            out.writeObject(new Respuesta("ERROR", "Suscripción inactiva. No puede reproducir contenido."));
            return false;
        }

        return true;
    }
}