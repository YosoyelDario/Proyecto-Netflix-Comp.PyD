package servidores;

import compartido.Usuario;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class RepositorioUsuarios {
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private final String archivoRuta;

    public RepositorioUsuarios(String archivoRuta) {
        this.archivoRuta = archivoRuta;
        cargarDesdeArchivo();
    }

    /**
     * Carga usuarios desde el archivo de texto (BD Usuarios).
     * Formato: username|password|plan|suscripcionActiva
     */
    private void cargarDesdeArchivo() {
        try (BufferedReader br = new BufferedReader(new FileReader(archivoRuta))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                linea = linea.trim();

                if (linea.isEmpty() || linea.startsWith("#")) {
                    continue;
                }

                String[] partes = linea.split("\\|");

                if (partes.length < 4) {
                    System.err.println("[BD-USUARIOS] Línea mal formada: " + linea);
                    continue;
                }

                String username = partes[0].trim();
                String password = partes[1].trim();
                String plan = partes[2].trim();
                boolean activa = Boolean.parseBoolean(partes[3].trim());

                usuarios.put(username.toLowerCase(), new Usuario(username, password, plan, activa));
            }

            System.out.println("[BD-USUARIOS] " + usuarios.size() + " usuarios cargados desde: " + archivoRuta);

        } catch (IOException e) {
            System.err.println("[BD-USUARIOS] Error al leer archivo: " + e.getMessage());
        }
    }

    /**
     * Autentica un usuario contra la BD (Fase 1 - Autenticación).
     * Retorna el usuario si las credenciales son válidas, null si no.
     */
    public synchronized Usuario autenticar(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        Usuario usuario = usuarios.get(username.toLowerCase());

        if (usuario == null) {
            return null;
        }

        if (!usuario.password.equals(password)) {
            return null;
        }

        return usuario;
    }
}
