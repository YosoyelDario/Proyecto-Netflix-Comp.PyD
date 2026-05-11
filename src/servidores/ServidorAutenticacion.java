package servidores;

import compartido.Peticion;
import compartido.Respuesta;
import compartido.Usuario;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Servidor B - Perfiles y Autenticación (Puerto 5100).
 *
 * Responsabilidades:
 * - Autenticar usuarios contra la BD (usuarios.txt)
 * - Generar y validar tokens de sesión
 * - Consultar datos de perfil
 *
 * Protocolo de comandos:
 * - LOGIN -> autentica y devuelve token
 * - VALIDAR_TOKEN -> verifica si un token es válido
 * - PERFIL -> devuelve datos del usuario asociado al token
 */
public class ServidorAutenticacion {
    private static int puerto = 5100;
    private static String rutaBD = "data/usuarios.txt";

    private static RepositorioUsuarios repoUsuarios;

    // Almacén de sesiones activas: token -> Usuario
    private static final Map<String, Usuario> sesionesActivas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.keyStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "123456");
    System.setProperty("javax.net.ssl.trustStore", "data/keystore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        if (args.length >= 1) {
            puerto = Integer.parseInt(args[0]);
        }

        if (args.length >= 2) {
            rutaBD = args[1];
        }

        repoUsuarios = new RepositorioUsuarios(rutaBD);
        
        try (ServerSocket serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(puerto)) {
            System.out.println("==================================================");
            System.out.println("[AUTH] Servidor de Autenticación iniciado");
            System.out.println("[AUTH] Escuchando en puerto: " + puerto);
            System.out.println("[AUTH] BD Usuarios: " + rutaBD);
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
                out.writeObject(new Respuesta("400", "Solicitud inválida."));
                out.flush();
                return;
            }

            Peticion peticion = (Peticion) recibido;
            String comando = peticion.comando.toUpperCase();

            System.out.println("[" + nombreHilo + "] Solicitud recibida: " + comando);

            switch (comando) {
                case "LOGIN":
                    procesarLogin(peticion, out, nombreHilo);
                    break;

                case "VALIDAR_TOKEN":
                    validarToken(peticion, out, nombreHilo);
                    break;

                case "PERFIL":
                    obtenerPerfil(peticion, out, nombreHilo);
                    break;

                default:
                    out.writeObject(new Respuesta("400", "Comando no soportado por autenticación."));
                    out.flush();
                    break;
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

    /**
     * Fase 1 - Autenticación.
     * Consulta BD Usuarios, genera token si es exitoso.
     */
    private static void procesarLogin(Peticion peticion, ObjectOutputStream out, String hilo) throws Exception {
        String[] partes = peticion.parametro.split(":");

        if (partes.length < 2) {
            out.writeObject(new Respuesta("400", "Formato de login inválido. Use usuario:contraseña."));
            out.flush();
            return;
        }

        String username = partes[0];
        String password = partes[1];

        // Consultar credenciales en BD Usuarios
        Usuario usuario = repoUsuarios.autenticar(username, password);

        if (usuario == null) {
            System.out.println("[" + hilo + "] Login fallido para: " + username);
            out.writeObject(new Respuesta("401", "Credenciales incorrectas."));
            out.flush();
            return;
        }

        // Generar token de sesión
        String token = UUID.randomUUID().toString();
        sesionesActivas.put(token, usuario);

        System.out.println("[" + hilo + "] Usuario autenticado: " + usuario.username + " | Token: " + token.substring(0, 8) + "...");

        // Enviar token + datos del usuario
        out.writeObject(new Respuesta("LOGIN_OK", token + "|" + usuario.username + "|" + usuario.plan + "|" + usuario.suscripcionActiva));
        out.flush();
    }

    /**
     * Valida si un token de sesión es activo.
     * Usado por otros servidores para verificar autorización.
     */
    private static void validarToken(Peticion peticion, ObjectOutputStream out, String hilo) throws Exception {
        String token = peticion.parametro;

        Usuario usuario = sesionesActivas.get(token);

        if (usuario == null) {
            out.writeObject(new Respuesta("401", "Sesión expirada o token inválido."));
            out.flush();
            System.out.println("[" + hilo + "] Token inválido recibido.");
            return;
        }

        out.writeObject(new Respuesta("OK", usuario));
        out.flush();
        System.out.println("[" + hilo + "] Token válido para: " + usuario.username);
    }

    /**
     * Retorna los datos de perfil asociados a un token.
     */
    private static void obtenerPerfil(Peticion peticion, ObjectOutputStream out, String hilo) throws Exception {
        String token = peticion.parametro;

        Usuario usuario = sesionesActivas.get(token);

        if (usuario == null) {
            out.writeObject(new Respuesta("401", "Sesión expirada."));
            out.flush();
            return;
        }

        out.writeObject(new Respuesta("OK", usuario.toString()));
        out.flush();
        System.out.println("[" + hilo + "] Perfil consultado: " + usuario.username);
    }
}
