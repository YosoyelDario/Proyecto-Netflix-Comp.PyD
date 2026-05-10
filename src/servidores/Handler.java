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

    private final String hostAuth;
    private final int puertoAuth;

    private Usuario usuarioActual;

    public Handler(
            Socket socket,
            RepositorioPeliculas repo,
            String ipCliente,
            String hostAuth,
            int puertoAuth
    ) {
        this.socket = socket;
        this.repo = repo;
        this.ipCliente = ipCliente;
        this.hostAuth = hostAuth;
        this.puertoAuth = puertoAuth;
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
        Respuesta respuestaAuth = solicitarAutenticacion(peticion);

        if (respuestaAuth == null) {
            out.writeObject(new Respuesta("ERROR", "Servidor de autenticación no disponible."));
            return;
        }

        if (!respuestaAuth.codigo.equals("LOGIN_OK")) {
            out.writeObject(respuestaAuth);
            return;
        }

        usuarioActual = (Usuario) respuestaAuth.payload;

        if (!usuarioActual.suscripcionActiva) {
            out.writeObject(new Respuesta(
                "LOGIN_OK",
                "Login correcto, pero la suscripción está inactiva."
            ));
            return;
        }

        out.writeObject(new Respuesta(
            "LOGIN_OK",
            "Bienvenido " + usuarioActual.username + ". " + usuarioActual.plan
        ));
    }

    private Respuesta solicitarAutenticacion(Peticion peticionLogin) {
        try (
            Socket socketAuth = new Socket(hostAuth, puertoAuth);
            ObjectOutputStream outAuth = new ObjectOutputStream(socketAuth.getOutputStream());
            ObjectInputStream inAuth = new ObjectInputStream(socketAuth.getInputStream())
        ) {
            outAuth.writeObject(peticionLogin);
            outAuth.flush();

            Object respuesta = inAuth.readObject();

            if (respuesta instanceof Respuesta) {
                return (Respuesta) respuesta;
            }

            return new Respuesta("ERROR", "Respuesta inválida del servidor de autenticación.");

        } catch (Exception e) {
            System.err.println("[AUTH-CLIENT] No se pudo contactar al servidor de autenticación: " + e.getMessage());
            return null;
        }
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