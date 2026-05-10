package servidores;

import compartido.Pelicula;
import compartido.Usuario;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositorioPeliculas {
    private final Map<String, Pelicula> peliculas = new HashMap<>();
    private final Map<String, Usuario> usuarios = new HashMap<>();

    public RepositorioPeliculas() {
        peliculas.put("matrix", new Pelicula("Matrix", 10));
        peliculas.put("inception", new Pelicula("Inception", 15));
        peliculas.put("interstellar", new Pelicula("Interstellar", 7));

        usuarios.put("vicente", new Usuario("vicente", "1234", "Plan Premium - Chile", true));
        usuarios.put("ana", new Usuario("ana", "abcd", "Plan Básico - Chile", true));
        usuarios.put("moroso", new Usuario("moroso", "1111", "Plan Premium - Chile", false));
    }

    public synchronized List<Pelicula> getLista() {
        return new ArrayList<>(peliculas.values());
    }

    public synchronized Pelicula buscar(String titulo) {
        if (titulo == null) {
            return null;
        }

        return peliculas.get(titulo.toLowerCase());
    }

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

    public synchronized String getPerfil(String username) {
        if (username == null) {
            return "Usuario inexistente";
        }

        Usuario usuario = usuarios.get(username.toLowerCase());

        if (usuario == null) {
            return "Usuario inexistente";
        }

        return usuario.toString();
    }
}