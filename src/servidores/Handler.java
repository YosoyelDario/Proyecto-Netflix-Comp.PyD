package servidores;

import compartido.Pelicula;
import compartido.Peticion;
import compartido.Respuesta;
import compartido.Usuario;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;

/**
 * Handler - Procesa UNA petición del Gateway ZUUL (o cliente directo).
 *
 * Modelo sin estado local: el estado del usuario vive en el token
 * de sesión gestionado por el Servidor B (Autenticación).
 * Cada petición incluye el token para identificar al usuario.
 *
 * Protocolo de códigos de respuesta:
 * - LOGIN_OK: autenticación exitosa (incluye token)
 * - OK: operación exitosa
 * - RESULTADOS: resultados de búsqueda
 * - STREAM_INFO: película autorizada para streaming
 * - 400: petición malformada
 * - 401: no autenticado / token inválido
 * - 403: acceso denegado (suscripción inactiva)
 * - 404: recurso no encontrado
 * - 500: error interno
 * - 503: servicio dependiente no disponible
 */
public class Handler implements Runnable {
    private final Socket socket;
    private final RepositorioPeliculas repo;
    private final String ipCliente;

    private final String hostAuth;
    private final int puertoAuth;

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
        String hilo = Thread.currentThread().getName();

        try (
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            Object recibido = in.readObject();

            if (!(recibido instanceof Peticion)) {
                out.writeObject(new Respuesta("400", "Petición inválida."));
                out.flush();
                return;
            }

            Peticion peticion = (Peticion) recibido;
            String comando = peticion.comando.toUpperCase();

            System.out.println(
                "[" + hilo + "] " + peticion.ipOrigen + " (vía Gateway) -> " + comando +
                (peticion.parametro.isEmpty() ? "" : " ('" + peticion.parametro + "')")
            );

            switch (comando) {
                case "LOGIN":
                    procesarLogin(peticion, out, hilo);
                    break;

                case "VER_CATALOGO":
                    procesarVerCatalogo(peticion, out, hilo);
                    break;

                case "BUSCAR":
                    procesarBusqueda(peticion, out, hilo);
                    break;

                case "VER_PERFIL":
                    procesarVerPerfil(peticion, out, hilo);
                    break;

                case "ELEGIR_PELICULA":
                    procesarElegirPelicula(peticion, out, hilo);
                    break;

                case "SALIR":
                    System.out.println("[" + hilo + "] Sesión cerrada desde " + ipCliente);
                    break;

                default:
                    out.writeObject(new Respuesta("400", "Comando no reconocido: " + comando));
                    break;
            }

            out.flush();

        } catch (Exception e) {
            System.err.println("[" + hilo + "] Error con " + ipCliente + ": " + e.getMessage());

        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // =========================================================================
    // FASE 1: AUTENTICACIÓN
    // =========================================================================

    private void procesarLogin(Peticion peticion, ObjectOutputStream out, String hilo) throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        Respuesta respuestaAuth = consultarServidorB(peticion);

        if (respuestaAuth == null) {
            out.writeObject(new Respuesta("503", "Servicio de autenticación no disponible. Reintente más tarde."));
            System.out.println("[" + hilo + "] ERROR 503: Servidor B no disponible.");
            return;
        }

        out.writeObject(respuestaAuth);

        if (respuestaAuth.codigo.equals("LOGIN_OK")) {
            String datos = (String) respuestaAuth.payload;
            String username = datos.split("\\|")[1];
            System.out.println("[" + hilo + "] Login OK: " + username);
        } else {
            System.out.println("[" + hilo + "] Login fallido.");
        }
    }

    // =========================================================================
    // FASE 2: CARGA DEL CATÁLOGO
    // =========================================================================

    private void procesarVerCatalogo(Peticion peticion, ObjectOutputStream out, String hilo) throws IOException {
        String token = peticion.parametro;

        Usuario usuario = validarTokenConServidorB(token, out, hilo);
        if (usuario == null) return;

        try {
            List<Pelicula> catalogo = repo.getLista();

            if (catalogo.isEmpty()) {
                out.writeObject(new Respuesta("500", "No se encontraron datos en el catálogo."));
                return;
            }

            out.writeObject(new Respuesta("OK", catalogo));
            System.out.println("[" + hilo + "] Catálogo enviado: " + catalogo.size() + " títulos.");

        } catch (Exception e) {
            out.writeObject(new Respuesta("500", "Error interno al cargar catálogo."));
        }
    }

    // =========================================================================
    // FASE 3: BÚSQUEDA DE TÍTULO
    // =========================================================================

    private void procesarBusqueda(Peticion peticion, ObjectOutputStream out, String hilo) throws IOException {
        // Formato del parámetro: "token|query"
        String[] partes = peticion.parametro.split("\\|", 2);
        String token = partes[0];
        String query = partes.length > 1 ? partes[1] : "";

        Usuario usuario = validarTokenConServidorB(token, out, hilo);
        if (usuario == null) return;

        if (query.trim().isEmpty()) {
            out.writeObject(new Respuesta("400", "Debe ingresar un término de búsqueda."));
            return;
        }

        try {
            List<Pelicula> resultados = repo.buscarPorQuery(query);

            if (resultados.isEmpty()) {
                out.writeObject(new Respuesta("OK", "No se encontraron coincidencias para: '" + query + "'"));
                System.out.println("[" + hilo + "] Búsqueda '" + query + "': 0 resultados.");
            } else {
                out.writeObject(new Respuesta("RESULTADOS", resultados));
                System.out.println("[" + hilo + "] Búsqueda '" + query + "': " + resultados.size() + " resultado(s).");
            }

        } catch (Exception e) {
            out.writeObject(new Respuesta("500", "Error interno en búsqueda."));
        }
    }

    // =========================================================================
    // VER PERFIL
    // =========================================================================

    private void procesarVerPerfil(Peticion peticion, ObjectOutputStream out, String hilo) throws IOException {
        String token = peticion.parametro;

        Respuesta respPerfil = consultarServidorB(new Peticion("PERFIL", token));

        if (respPerfil == null) {
            out.writeObject(new Respuesta("503", "Servicio de perfiles no disponible."));
            return;
        }

        out.writeObject(respPerfil);
        System.out.println("[" + hilo + "] Perfil enviado.");
    }

    // =========================================================================
    // ELEGIR PELÍCULA (autorizar streaming - Función B)
    // =========================================================================

    private void procesarElegirPelicula(Peticion peticion, ObjectOutputStream out, String hilo) throws IOException {
        // Formato: "token|titulo"
        String[] partes = peticion.parametro.split("\\|", 2);
        String token = partes[0];
        String titulo = partes.length > 1 ? partes[1] : "";

        Usuario usuario = validarTokenConServidorB(token, out, hilo);
        if (usuario == null) return;

        if (!usuario.suscripcionActiva) {
            out.writeObject(new Respuesta("403", "No tiene acceso a este contenido. Suscripción inactiva."));
            System.out.println("[" + hilo + "] ERROR 403: Suscripción inactiva para " + usuario.username);
            return;
        }

        Pelicula pelicula = repo.buscarExacto(titulo);

        if (pelicula == null) {
            out.writeObject(new Respuesta("404", "Película no encontrada: '" + titulo + "'"));
            System.out.println("[" + hilo + "] ERROR 404: '" + titulo + "' no existe.");
            return;
        }

        out.writeObject(new Respuesta("STREAM_INFO", pelicula));
        System.out.println("[" + hilo + "] Streaming autorizado: " + pelicula.titulo);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private Usuario validarTokenConServidorB(String token, ObjectOutputStream out, String hilo) throws IOException {
        if (token == null || token.trim().isEmpty()) {
            out.writeObject(new Respuesta("401", "Debe iniciar sesión antes de usar el sistema."));
            return null;
        }

        Respuesta resp = consultarServidorB(new Peticion("VALIDAR_TOKEN", token));

        if (resp == null) {
            out.writeObject(new Respuesta("503", "No se pudo verificar sesión. Servicio no disponible."));
            return null;
        }

        if (!resp.codigo.equals("OK")) {
            out.writeObject(new Respuesta("401", "Sesión expirada. Inicie sesión nuevamente."));
            return null;
        }

        return (Usuario) resp.payload;
    }

    private Respuesta consultarServidorB(Peticion peticion) {
        try (
            Socket socketAuth = SSLSocketFactory.getDefault().createSocket(hostAuth, puertoAuth);
        ObjectOutputStream outAuth = new ObjectOutputStream(socketAuth.getOutputStream());
            ObjectInputStream inAuth = new ObjectInputStream(socketAuth.getInputStream())
        ) {
            outAuth.writeObject(peticion);
            outAuth.flush();

            Object respuesta = inAuth.readObject();

            if (respuesta instanceof Respuesta) {
                return (Respuesta) respuesta;
            }

            return new Respuesta("500", "Respuesta inválida del Servidor B.");

        } catch (Exception e) {
            System.err.println("[HANDLER] No se pudo contactar Servidor B: " + e.getMessage());
            return null;
        }
    }
}