package servidores;

import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Handler implements Runnable {
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
                    case "VER_CATALOGO":
                        out.writeObject(new Respuesta("OK", repo.getLista()));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada: Catálogo.");
                        break;

                    case "VER_PERFIL":
                        out.writeObject(new Respuesta("OK", repo.getPerfil(peticion.parametro)));
                        System.out.println("[" + nombreHilo + "] Respuesta enviada: Perfil.");
                        break;

                    case "ELEGIR_PELICULA":
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
}